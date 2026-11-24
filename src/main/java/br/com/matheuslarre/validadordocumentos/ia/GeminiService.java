package br.com.matheuslarre.validadordocumentos.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta";

    // Prompt no estilo do exemplo do mentor: campos definidos, só JSON, null quando não encontrar, sem inventar
    private static final String PROMPT = """
            Você extrai dados de comprovantes de residência brasileiros.
            O TEXTO abaixo foi obtido por OCR e pode conter pequenos erros de leitura.
            Extraia os campos: nome, cpf, endereco, cep, cidade, estado (sigla com 2 letras)
            e dataEmissao (data de emissão do documento, no formato AAAA-MM-DD).
            Responda APENAS com um objeto JSON contendo exatamente essas chaves.
            Se não encontrar um campo, use null. Não invente informações.

            TEXTO:
            %s
            """;

    // Mesmo estilo de prompt, agora para documentos de identificação
    private static final String PROMPT_IDENTIFICACAO = """
            Você extrai dados de documentos de identificação brasileiros (RG, CIN ou CNH).
            O TEXTO abaixo foi obtido por OCR e pode conter pequenos erros de leitura.
            Extraia os campos: nome (nome completo do titular), cpf,
            numeroDocumento (número do RG ou número de registro da CNH),
            tipoIdentificado ("RG" ou "CNH"; para CIN use "RG"),
            dataNascimento e dataValidade (validade do documento; o RG normalmente não tem),
            com as datas no formato AAAA-MM-DD.
            Responda APENAS com um objeto JSON contendo exatamente essas chaves.
            Se não encontrar um campo, use null. Não invente informações.

            TEXTO:
            %s
            """;

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    // Erros temporários (servidor sobrecarregado / limite por minuto) valem uma nova tentativa
    private static final int MAX_TENTATIVAS = 2;
    private static final Duration ESPERA_ENTRE_TENTATIVAS = Duration.ofSeconds(3);
    private static final Duration TIMEOUT_CONEXAO = Duration.ofSeconds(10);
    private static final Duration TIMEOUT_RESPOSTA = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String modelo;
    private final Duration esperaEntreTentativas;

    @Autowired // com dois construtores, este é o que o Spring usa
    public GeminiService(JsonMapper jsonMapper,
                         @Value("${gemini.api-key}") String apiKey,
                         @Value("${gemini.modelo}") String modelo) {
        this(RestClient.builder().requestFactory(fabricaComTimeout()), jsonMapper, apiKey, modelo, ESPERA_ENTRE_TENTATIVAS);
    }

    /** Usado nos testes: recebe um builder ligado a um servidor simulado e uma espera curta. */
    GeminiService(RestClient.Builder builder, JsonMapper jsonMapper, String apiKey, String modelo,
                  Duration esperaEntreTentativas) {
        this.restClient = builder.baseUrl(URL_BASE).build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.esperaEntreTentativas = esperaEntreTentativas;
    }

    // Sem timeout, uma API travada deixaria o upload do usuário "carregando" para sempre
    private static SimpleClientHttpRequestFactory fabricaComTimeout() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TIMEOUT_CONEXAO);
        fabrica.setReadTimeout(TIMEOUT_RESPOSTA);
        return fabrica;
    }

    /** Envia o texto do OCR ao Gemini e devolve os dados estruturados. */
    public DadosComprovante extrairDados(String textoOcr) {
        return extrair(PROMPT, textoOcr, DadosComprovante.class);
    }

    /** Envia o texto do OCR de um RG/CNH ao Gemini e devolve os dados de identificação. */
    public DadosIdentificacao extrairIdentificacao(String textoOcr) {
        return extrair(PROMPT_IDENTIFICACAO, textoOcr, DadosIdentificacao.class);
    }

    /**
     * Fluxo comum aos dois tipos de documento: muda só o prompt e o record de destino.
     * <T> é um tipo genérico: o mesmo método devolve DadosComprovante ou DadosIdentificacao.
     */
    private <T> T extrair(String prompt, String textoOcr, Class<T> tipo) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ExtracaoException("Chave do Gemini não configurada. Defina GEMINI_API_KEY no arquivo .env.");
        }

        // Corpo da requisição no formato da API do Gemini
        Map<String, Object> corpo = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt.formatted(textoOcr))))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json", // pede JSON puro na resposta
                        "temperature", 0                        // respostas mais previsíveis
                )
        );

        return converterResposta(extrairTexto(chamarComNovaTentativa(corpo)), tipo);
    }

    private RespostaGemini chamarComNovaTentativa(Map<String, Object> corpo) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return restClient.post()
                        .uri("/models/{modelo}:generateContent", modelo)
                        .header("x-goog-api-key", apiKey) // a chave vai no cabeçalho, nunca na URL
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(corpo)
                        .retrieve()
                        .body(RespostaGemini.class);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                // O detalhe técnico vai para o log (para o desenvolvedor); o usuário recebe uma mensagem clara
                log.warn("Gemini respondeu {} na tentativa {}/{}: {}", status, tentativa, MAX_TENTATIVAS, e.getStatusText());
                if (ehTemporario(status) && tentativa < MAX_TENTATIVAS) {
                    esperar();
                    continue;
                }
                throw new ExtracaoException(mensagemParaStatus(status), e);
            } catch (ResourceAccessException e) {
                log.warn("Gemini não respondeu a tempo ou está inacessível: {}", e.getMessage());
                throw new ExtracaoException("O serviço de IA demorou demais para responder. Tente novamente.", e);
            } catch (RestClientException e) {
                log.warn("Falha inesperada ao consultar o Gemini", e);
                throw new ExtracaoException("Não foi possível consultar o serviço de IA. Tente novamente.", e);
            }
        }
    }

    private static boolean ehTemporario(int status) {
        return status == 429 || status == 503;
    }

    static String mensagemParaStatus(int status) {
        return switch (status) {
            case 503 -> "O serviço de IA está sobrecarregado no momento. Tente novamente em alguns minutos.";
            case 429 -> "Limite de uso da IA atingido. Aguarde um minuto e tente novamente.";
            case 400, 401, 403 -> "A chave do Gemini é inválida ou não tem permissão. Confira GEMINI_API_KEY no .env.";
            case 404 -> "Modelo de IA não encontrado. Confira GEMINI_MODEL no .env.";
            default -> "O serviço de IA respondeu com erro (" + status + "). Tente novamente.";
        };
    }

    private void esperar() {
        try {
            Thread.sleep(esperaEntreTentativas.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // respeita quem pediu a interrupção
            throw new ExtracaoException("A consulta à IA foi interrompida.", e);
        }
    }

    /** Converte o JSON devolvido pela IA no record. Sem "private" para poder ser testado. */
    DadosComprovante converterResposta(String json) {
        return converterResposta(json, DadosComprovante.class);
    }

    <T> T converterResposta(String json, Class<T> tipo) {
        try {
            return jsonMapper.readValue(removerBlocoDeCodigo(json), tipo);
        } catch (JacksonException e) {
            throw new ExtracaoException("O Gemini devolveu uma resposta que não é um JSON válido.", e);
        }
    }

    // Algumas respostas vêm embrulhadas em ```json ... ```: removemos esse "embrulho"
    private String removerBlocoDeCodigo(String json) {
        String texto = json == null ? "" : json.strip();
        if (texto.startsWith("```")) {
            texto = texto.replaceFirst("^```(json)?", "").replaceFirst("```$", "").strip();
        }
        return texto;
    }

    private String extrairTexto(RespostaGemini resposta) {
        if (resposta == null || resposta.candidates() == null || resposta.candidates().isEmpty()) {
            throw new ExtracaoException("O Gemini não devolveu nenhuma resposta.");
        }
        Conteudo conteudo = resposta.candidates().get(0).content();
        if (conteudo == null || conteudo.parts() == null || conteudo.parts().isEmpty()) {
            throw new ExtracaoException("A resposta do Gemini veio vazia.");
        }
        return conteudo.parts().get(0).text();
    }

    // Formato (simplificado) da resposta da API: candidates[0].content.parts[0].text
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaGemini(List<Candidato> candidates) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidato(Conteudo content) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Conteudo(List<Parte> parts) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Parte(String text) { }
}

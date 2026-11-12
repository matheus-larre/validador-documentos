package br.com.matheuslarre.validadordocumentos.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

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

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String modelo;

    public GeminiService(JsonMapper jsonMapper,
                         @Value("${gemini.api-key}") String apiKey,
                         @Value("${gemini.modelo}") String modelo) {
        this.restClient = RestClient.create(URL_BASE);
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    /** Envia o texto do OCR ao Gemini e devolve os dados estruturados. */
    public DadosComprovante extrairDados(String textoOcr) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ExtracaoException("Chave do Gemini não configurada. Defina GEMINI_API_KEY no arquivo .env.");
        }

        // Corpo da requisição no formato da API do Gemini
        Map<String, Object> corpo = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", PROMPT.formatted(textoOcr))))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json", // pede JSON puro na resposta
                        "temperature", 0                        // respostas mais previsíveis
                )
        );

        try {
            RespostaGemini resposta = restClient.post()
                    .uri("/models/{modelo}:generateContent", modelo)
                    .header("x-goog-api-key", apiKey) // a chave vai no cabeçalho, nunca na URL
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(RespostaGemini.class);

            return converterResposta(extrairTexto(resposta));
        } catch (RestClientException e) {
            throw new ExtracaoException("Falha ao consultar o Gemini: " + e.getMessage(), e);
        }
    }

    /** Converte o JSON devolvido pela IA no record. Sem "private" para poder ser testado. */
    DadosComprovante converterResposta(String json) {
        try {
            return jsonMapper.readValue(removerBlocoDeCodigo(json), DadosComprovante.class);
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

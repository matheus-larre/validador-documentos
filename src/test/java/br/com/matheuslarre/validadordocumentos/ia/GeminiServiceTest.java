package br.com.matheuslarre.validadordocumentos.ia;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiServiceTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final GeminiService service = new GeminiService(jsonMapper, "chave-falsa", "modelo-teste");

    @Test
    void deveConverterJsonDoGeminiEmDadosDoComprovante() {
        String json = """
                {"nome":"João da Silva","cpf":"123.456.789-00","endereco":"Rua das Flores, 150",
                 "cep":"46430-000","cidade":"Guanambi","estado":"BA","dataEmissao":"2026-08-10"}
                """;

        DadosComprovante dados = service.converterResposta(json);

        assertEquals("João da Silva", dados.nome());
        assertEquals("123.456.789-00", dados.cpf());
        assertEquals("Guanambi", dados.cidade());
        assertEquals("BA", dados.estado());
        assertEquals(LocalDate.of(2026, 8, 10), dados.dataEmissao());
    }

    @Test
    void deveAceitarJsonDentroDeBlocoDeCodigoECamposNulos() {
        String json = """
                ```json
                {"nome":"Maria Souza","cpf":null,"endereco":null,"cep":null,"cidade":null,"estado":null,"dataEmissao":null}
                ```""";

        DadosComprovante dados = service.converterResposta(json);

        assertEquals("Maria Souza", dados.nome());
        assertNull(dados.cpf());
        assertNull(dados.dataEmissao());
    }

    @Test
    void deveFalharQuandoRespostaNaoForJson() {
        assertThrows(ExtracaoException.class, () -> service.converterResposta("isso não é um JSON"));
    }

    @Test
    void deveExigirChaveConfigurada() {
        GeminiService semChave = new GeminiService(jsonMapper, "", "modelo-teste");

        ExtracaoException erro = assertThrows(ExtracaoException.class, () -> semChave.extrairDados("texto qualquer"));
        assertTrue(erro.getMessage().contains("GEMINI_API_KEY"));
    }

    // ===== Chamada HTTP com um servidor simulado (MockRestServiceServer): a API real nunca é chamada =====

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/modelo-teste:generateContent";

    private static final String RESPOSTA_OK = """
            {"candidates":[{"content":{"parts":[{"text":"{\\"nome\\":\\"Maria Teste\\",\\"estado\\":\\"PE\\"}"}]}}]}
            """;

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    private final GeminiService comServidorSimulado =
            new GeminiService(builder, jsonMapper, "chave-falsa", "modelo-teste", Duration.ZERO);

    @Test
    void deveEnviarChaveNoCabecalhoELerResposta() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "chave-falsa"))
                .andRespond(withSuccess(RESPOSTA_OK, MediaType.APPLICATION_JSON));

        DadosComprovante dados = comServidorSimulado.extrairDados("texto do OCR");

        assertEquals("Maria Teste", dados.nome());
        assertEquals("PE", dados.estado());
        servidor.verify();
    }

    @Test
    void deveTentarDeNovoQuandoGeminiEstiverSobrecarregado() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        servidor.expect(requestTo(URL)).andRespond(withSuccess(RESPOSTA_OK, MediaType.APPLICATION_JSON));

        DadosComprovante dados = comServidorSimulado.extrairDados("texto do OCR");

        assertEquals("Maria Teste", dados.nome());
        servidor.verify(); // confirma que houve exatamente 2 chamadas
    }

    @Test
    void deveDesistirComMensagemAmigavelSeContinuarSobrecarregado() {
        servidor.expect(ExpectedCount.times(2), requestTo(URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("{\"error\":{\"code\":503}}"));

        ExtracaoException erro = assertThrows(ExtracaoException.class,
                () -> comServidorSimulado.extrairDados("texto do OCR"));

        assertEquals("O serviço de IA está sobrecarregado no momento. Tente novamente em alguns minutos.",
                erro.getMessage());
        assertFalse(erro.getMessage().contains("{"), "o JSON cru do Google não deve chegar ao usuário");
        servidor.verify();
    }

    @Test
    void naoDeveTentarDeNovoQuandoModeloNaoExistir() {
        servidor.expect(ExpectedCount.once(), requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        ExtracaoException erro = assertThrows(ExtracaoException.class,
                () -> comServidorSimulado.extrairDados("texto do OCR"));

        assertTrue(erro.getMessage().contains("GEMINI_MODEL"));
        servidor.verify(); // 404 não é temporário: só 1 chamada
    }
}

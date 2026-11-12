package br.com.matheuslarre.validadordocumentos.ia;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

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
}

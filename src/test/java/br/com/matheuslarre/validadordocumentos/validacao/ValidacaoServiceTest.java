package br.com.matheuslarre.validadordocumentos.validacao;

import br.com.matheuslarre.validadordocumentos.ia.DadosComprovante;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários das regras: sem Spring, sem banco, sem IA.
 * O relógio é fixo em 24/11/2026, então os testes de data dão sempre o mesmo resultado.
 */
class ValidacaoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 24);
    private static final Clock RELOGIO_FIXO = Clock.fixed(HOJE.atStartOfDay(FUSO).toInstant(), FUSO);

    private static final String TEXTO_OCR = "CONTA DE ENERGIA ELETRICA - Nome: MARIA TESTE DA SILVA - CPF 529.982.247-25";

    private final ValidacaoService service = new ValidacaoService(RELOGIO_FIXO);

    private static DadosComprovante dadosValidos() {
        return new DadosComprovante("MARIA TESTE DA SILVA", "529.982.247-25", "RUA DAS FLORES, 123",
                "51020-000", "RECIFE", "PE", HOJE.minusDays(14));
    }

    // ===== Decisão final =====

    @Nested
    class DecisaoFinal {

        @Test
        void deveAprovarQuandoTodasAsRegrasPassam() {
            ResultadoValidacao resultado = service.validar(TEXTO_OCR, dadosValidos());

            assertEquals(StatusValidacao.APROVADO, resultado.status());
            assertEquals(7, resultado.regras().size());
            assertTrue(resultado.regras().stream().allMatch(RegraResultado::aprovada));
        }

        @Test
        void deveReprovarQuandoUmaUnicaRegraFalha() {
            DadosComprovante semCep = new DadosComprovante("MARIA", "529.982.247-25", "RUA X, 1",
                    null, "RECIFE", "PE", HOJE);

            ResultadoValidacao resultado = service.validar(TEXTO_OCR, semCep);

            assertEquals(StatusValidacao.REPROVADO, resultado.status());
            assertEquals(1, resultado.regras().stream().filter(r -> !r.aprovada()).count());
        }

        @Test
        void deveReprovarDocumentoIlegivelSemDadosDaIa() {
            ResultadoValidacao resultado = service.validar("", null);

            assertEquals(StatusValidacao.REPROVADO, resultado.status());
            assertFalse(resultado.regras().get(0).aprovada(), "a regra de legibilidade deve falhar");
        }
    }

    // ===== Regra 1: documento legível =====

    @Nested
    class DocumentoLegivel {

        @Test
        void devePassarComTrintaCaracteresNaoBrancos() {
            String trinta = "a".repeat(15) + "   \n  " + "b".repeat(15); // espaços não contam
            assertTrue(service.documentoLegivel(trinta).aprovada());
        }

        @Test
        void deveFalharComVinteENoveCaracteres() {
            assertFalse(service.documentoLegivel("a".repeat(29)).aprovada());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\n\n\t"})
        void deveFalharSemTexto(String texto) {
            assertFalse(service.documentoLegivel(texto).aprovada());
        }
    }

    // ===== Regras 2 e 4: nome e endereço =====

    @Nested
    class NomeEEndereco {

        @Test
        void devePassarComNomeEEnderecoPreenchidos() {
            assertTrue(service.nomeIdentificado("MARIA TESTE").aprovada());
            assertTrue(service.enderecoIdentificado("RUA DAS FLORES, 123").aprovada());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void deveFalharSemNomeOuEndereco(String valor) {
            assertFalse(service.nomeIdentificado(valor).aprovada());
            assertFalse(service.enderecoIdentificado(valor).aprovada());
        }
    }

    // ===== Regra 3: CPF (módulo 11) =====

    @Nested
    class Cpf {

        @ParameterizedTest
        @ValueSource(strings = {
                "529.982.247-25",  // com pontuação
                "52998224725",     // só dígitos
                "100.000.001-08"   // 1º dígito verificador é 0 (caso "resto < 2" do algoritmo)
        })
        void deveAceitarCpfComDigitosCorretos(String cpf) {
            assertTrue(service.cpfValido(cpf).aprovada());
        }

        @Test
        void deveRecusarCpfComDigitoVerificadorErrado() {
            RegraResultado regra = service.cpfValido("529.982.247-26");

            assertFalse(regra.aprovada());
            assertTrue(regra.motivo().contains("dígitos verificadores"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"111.111.111-11", "000.000.000-00", "999.999.999-99"})
        void deveRecusarSequenciaRepetida(String cpf) {
            assertFalse(service.cpfValido(cpf).aprovada());
        }

        @ParameterizedTest
        @ValueSource(strings = {"529.982.247-2", "529.982.247-255", "abc"})
        void deveRecusarCpfSemOnzeDigitos(String cpf) {
            assertFalse(service.cpfValido(cpf).aprovada());
        }

        @ParameterizedTest
        @NullAndEmptySource
        void deveRecusarCpfAusente(String cpf) {
            assertFalse(service.cpfValido(cpf).aprovada());
        }
    }

    // ===== Regra 5: CEP =====

    @Nested
    class Cep {

        @ParameterizedTest
        @ValueSource(strings = {"51020-000", "51020000", "51.020-000"})
        void deveAceitarCepComOitoDigitos(String cep) {
            assertTrue(service.cepValido(cep).aprovada());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"5102-000", "510200001"})
        void deveRecusarCepInvalidoOuAusente(String cep) {
            assertFalse(service.cepValido(cep).aprovada());
        }
    }

    // ===== Regra 6: Estado =====

    @Nested
    class Estado {

        @ParameterizedTest
        @ValueSource(strings = {"PE", "pe", " SP ", "DF"})
        void deveAceitarUfValida(String uf) {
            assertTrue(service.estadoValido(uf).aprovada());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"XX", "Pernambuco", "P"})
        void deveRecusarUfInvalidaOuAusente(String uf) {
            assertFalse(service.estadoValido(uf).aprovada());
        }
    }

    // ===== Regra 7: emissão nos últimos 90 dias =====

    @Nested
    class Emissao {

        @Test
        void deveAceitarEmitidoHoje() {
            assertTrue(service.emissaoRecente(HOJE).aprovada());
        }

        @Test
        void deveAceitarEmitidoHaExatamenteNoventaDias() {
            assertTrue(service.emissaoRecente(HOJE.minusDays(90)).aprovada());
        }

        @Test
        void deveRecusarEmitidoHaNoventaEUmDias() {
            RegraResultado regra = service.emissaoRecente(HOJE.minusDays(91));

            assertFalse(regra.aprovada());
            assertTrue(regra.motivo().contains("91 dias"));
        }

        @Test
        void deveRecusarDataNoFuturo() {
            RegraResultado regra = service.emissaoRecente(HOJE.plusDays(1));

            assertFalse(regra.aprovada());
            assertTrue(regra.motivo().contains("futuro"));
        }

        @Test
        void deveRecusarSemData() {
            assertFalse(service.emissaoRecente(null).aprovada());
        }
    }
}

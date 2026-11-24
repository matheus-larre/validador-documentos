package br.com.matheuslarre.validadordocumentos.service;

import br.com.matheuslarre.validadordocumentos.ia.DadosComprovante;
import br.com.matheuslarre.validadordocumentos.ia.DadosIdentificacao;
import br.com.matheuslarre.validadordocumentos.ia.ExtracaoException;
import br.com.matheuslarre.validadordocumentos.ia.GeminiService;
import br.com.matheuslarre.validadordocumentos.model.Documento;
import br.com.matheuslarre.validadordocumentos.model.TipoDocumento;
import br.com.matheuslarre.validadordocumentos.repository.DocumentoRepository;
import br.com.matheuslarre.validadordocumentos.validacao.StatusValidacao;
import br.com.matheuslarre.validadordocumentos.validacao.ValidacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Teste unitário da orquestração com Mockito: armazenamento, OCR, IA e banco são "dublês" (mocks).
 * Só o ValidacaoService é real, com o relógio fixo em 24/11/2026.
 */
class ProcessamentoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 24);
    private static final Clock RELOGIO = Clock.fixed(HOJE.atStartOfDay(FUSO).toInstant(), FUSO);
    private static final String TEXTO_OCR = "CONTA DE ENERGIA - MARIA TESTE DA SILVA - CPF 529.982.247-25 - RECIFE PE";

    private final ArmazenamentoService armazenamento = mock(ArmazenamentoService.class);
    private final OcrService ocr = mock(OcrService.class);
    private final GeminiService gemini = mock(GeminiService.class);
    private final DocumentoRepository repository = mock(DocumentoRepository.class);

    private final ProcessamentoService service = new ProcessamentoService(
            armazenamento, ocr, gemini, new ValidacaoService(RELOGIO), repository, RELOGIO);

    private final MockMultipartFile arquivo =
            new MockMultipartFile("arquivo", "conta.png", "image/png", new byte[]{1, 2, 3});

    @BeforeEach
    void configurar() {
        when(armazenamento.salvar(any())).thenReturn(Path.of("uploads", "uuid.png"));
        when(repository.save(any(Documento.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private Documento documentoGravado() {
        ArgumentCaptor<Documento> captor = ArgumentCaptor.forClass(Documento.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void deveGravarDocumentoAprovadoComDadosEChecklist() {
        when(ocr.extrairTexto(any())).thenReturn(TEXTO_OCR);
        when(gemini.extrairDados(TEXTO_OCR)).thenReturn(new DadosComprovante("MARIA TESTE DA SILVA",
                "529.982.247-25", "RUA DAS FLORES, 123", "51020-000", "RECIFE", "PE", HOJE.minusDays(14)));

        ResultadoProcessamento resultado = service.processar("COMPROVANTE_RESIDENCIA", arquivo);

        assertEquals(StatusValidacao.APROVADO, resultado.validacao().status());
        Documento gravado = documentoGravado();
        assertEquals("conta.png", gravado.getNomeArquivoOriginal());
        assertEquals("uuid.png", gravado.getNomeArquivoSalvo());
        assertEquals("MARIA TESTE DA SILVA", gravado.getNome());
        assertEquals(StatusValidacao.APROVADO, gravado.getStatus());
        assertEquals(7, gravado.getRegras().size());
        assertEquals(HOJE.atStartOfDay(), gravado.getCriadoEm());
    }

    @Test
    void deveGravarSemStatusQuandoIaFalhar() {
        when(ocr.extrairTexto(any())).thenReturn(TEXTO_OCR);
        when(gemini.extrairDados(anyString())).thenThrow(new ExtracaoException("IA sobrecarregada"));

        ResultadoProcessamento resultado = service.processar("COMPROVANTE_RESIDENCIA", arquivo);

        assertNull(resultado.validacao(), "falha da IA não deve reprovar o documento");
        assertEquals("IA sobrecarregada", resultado.erroIa());
        Documento gravado = documentoGravado();
        assertNull(gravado.getStatus());
        assertEquals("IA sobrecarregada", gravado.getErroProcessamento());
    }

    @Test
    void deveReprovarDocumentoIlegivelSemChamarIa() {
        when(ocr.extrairTexto(any())).thenReturn("");

        ResultadoProcessamento resultado = service.processar("COMPROVANTE_RESIDENCIA", arquivo);

        assertEquals(StatusValidacao.REPROVADO, resultado.validacao().status());
        verifyNoInteractions(gemini); // não gasta chamada da IA com documento em branco
        assertEquals(StatusValidacao.REPROVADO, documentoGravado().getStatus());
    }

    @Test
    void naoDeveGravarNadaQuandoArquivoForInvalido() {
        when(armazenamento.salvar(any())).thenThrow(new IllegalArgumentException("Formato não suportado."));

        assertThrows(IllegalArgumentException.class, () -> service.processar("COMPROVANTE_RESIDENCIA", arquivo));
        verifyNoInteractions(ocr, gemini, repository);
    }

    @Test
    void deveUsarPromptERegrasDeIdentificacaoParaRgOuCnh() {
        when(ocr.extrairTexto(any())).thenReturn(TEXTO_OCR);
        when(gemini.extrairIdentificacao(TEXTO_OCR)).thenReturn(new DadosIdentificacao("MARIA TESTE DA SILVA",
                "529.982.247-25", "01234567890", "CNH", LocalDate.of(1995, 3, 10), HOJE.plusYears(3)));

        ResultadoProcessamento resultado = service.processar("DOCUMENTO_IDENTIFICACAO", arquivo);

        assertEquals(StatusValidacao.APROVADO, resultado.validacao().status());
        verify(gemini, never()).extrairDados(anyString()); // não usa o prompt do comprovante
        Documento gravado = documentoGravado();
        assertEquals(TipoDocumento.DOCUMENTO_IDENTIFICACAO, gravado.getTipo());
        assertEquals("01234567890", gravado.getNumeroDocumento());
        assertEquals("CNH", gravado.getTipoIdentificado());
        assertEquals(LocalDate.of(1995, 3, 10), gravado.getDataNascimento());
        assertNull(gravado.getCep(), "campos do comprovante ficam vazios");
    }

    @Test
    void naoDeveNemSalvarOArquivoComTipoDesconhecido() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.processar("PASSAPORTE", arquivo));

        assertTrue(erro.getMessage().contains("não suportado"));
        verifyNoInteractions(armazenamento, ocr, gemini, repository);
    }
}

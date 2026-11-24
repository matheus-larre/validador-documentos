package br.com.matheuslarre.validadordocumentos.api;

import br.com.matheuslarre.validadordocumentos.ia.DadosComprovante;
import br.com.matheuslarre.validadordocumentos.ia.GeminiService;
import br.com.matheuslarre.validadordocumentos.model.Documento;
import br.com.matheuslarre.validadordocumentos.repository.DocumentoRepository;
import br.com.matheuslarre.validadordocumentos.service.OcrService;
import br.com.matheuslarre.validadordocumentos.validacao.StatusValidacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes da API REST com MockMvc: simula requisições HTTP sem abrir uma porta de verdade.
 * Sobe o Spring inteiro (perfil "test", com H2), mas troca OCR e IA por mocks:
 * o teste não depende do Tesseract instalado nem da API do Google.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentoApiControllerTest {

    private static final String TEXTO_OCR = "CONTA DE ENERGIA - MARIA TESTE DA SILVA - CPF 529.982.247-25 - RECIFE PE";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DocumentoRepository repository;

    @MockitoBean
    private OcrService ocrService;

    @MockitoBean
    private GeminiService geminiService;

    private final MockMultipartFile png =
            new MockMultipartFile("arquivo", "conta.png", "image/png", new byte[]{1, 2, 3});

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void postDeveProcessarEResponder201ComLocation() throws Exception {
        LocalDate emissao = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(5);
        when(ocrService.extrairTexto(any())).thenReturn(TEXTO_OCR);
        when(geminiService.extrairDados(anyString())).thenReturn(new DadosComprovante("MARIA TESTE DA SILVA",
                "529.982.247-25", "RUA DAS FLORES, 123", "51020-000", "RECIFE", "PE", emissao));

        mvc.perform(multipart("/api/documentos").file(png).param("tipo", "COMPROVANTE_RESIDENCIA"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/documentos/")))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.arquivo").value("conta.png"))
                .andExpect(jsonPath("$.regras.length()").value(7))
                .andExpect(jsonPath("$.dados.nome").value("MARIA TESTE DA SILVA"));
    }

    @Test
    void postComFormatoNaoSuportadoDeveResponder400EmProblemDetail() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("arquivo", "nota.txt", "text/plain", "oi".getBytes());

        mvc.perform(multipart("/api/documentos").file(txt).param("tipo", "COMPROVANTE_RESIDENCIA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail").value(containsString("Formato não suportado")));
    }

    @Test
    void postSemArquivoDeveResponder400() throws Exception {
        mvc.perform(multipart("/api/documentos").param("tipo", "COMPROVANTE_RESIDENCIA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void getDeveListarHistoricoResumido() throws Exception {
        Documento doc = new Documento("conta.png", "uuid.png", "COMPROVANTE_RESIDENCIA", LocalDateTime.now());
        doc.setNome("MARIA TESTE");
        doc.setStatus(StatusValidacao.REPROVADO);
        repository.save(doc);

        mvc.perform(get("/api/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].arquivo").value("conta.png"))
                .andExpect(jsonPath("$[0].titular").value("MARIA TESTE"))
                .andExpect(jsonPath("$[0].status").value("REPROVADO"))
                .andExpect(jsonPath("$[0].textoOcr").doesNotExist()); // o resumo não traz o texto inteiro
    }

    @Test
    void getPorIdDeveDevolverDetalhe() throws Exception {
        Documento doc = repository.save(
                new Documento("conta.png", "uuid.png", "COMPROVANTE_RESIDENCIA", LocalDateTime.now()));

        mvc.perform(get("/api/documentos/{id}", doc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doc.getId().toString()))
                .andExpect(jsonPath("$.arquivo").value("conta.png"));
    }

    @Test
    void getPorIdInexistenteDeveResponder404EmProblemDetail() throws Exception {
        mvc.perform(get("/api/documentos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Documento não encontrado"));
    }

    @Test
    void getComIdMalformadoDeveResponder404() throws Exception {
        mvc.perform(get("/api/documentos/abc"))
                .andExpect(status().isNotFound());
    }
}

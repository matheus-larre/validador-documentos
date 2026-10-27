package br.com.matheuslarre.validadordocumentos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ArmazenamentoServiceTest {

    @TempDir
    Path pastaTemporaria; // o JUnit cria e apaga essa pasta automaticamente

    private ArmazenamentoService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new ArmazenamentoService(pastaTemporaria.toString());
    }

    @Test
    void deveSalvarArquivoValidoComNomeAleatorio() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "conta-de-luz.png", "image/png", new byte[]{1, 2, 3});

        Path salvo = service.salvar(arquivo);

        assertTrue(Files.exists(salvo), "o arquivo deveria existir no disco");
        assertTrue(salvo.getFileName().toString().endsWith(".png"), "deveria manter a extensão");
        assertNotEquals("conta-de-luz.png", salvo.getFileName().toString(), "o nome deveria ser um UUID");
    }

    @Test
    void deveRejeitarArquivoVazio() {
        MockMultipartFile vazio = new MockMultipartFile(
                "arquivo", "vazio.pdf", "application/pdf", new byte[0]);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.salvar(vazio));
        assertEquals("Selecione um arquivo antes de enviar.", erro.getMessage());
    }

    @Test
    void deveRejeitarFormatoNaoSuportado() {
        MockMultipartFile texto = new MockMultipartFile(
                "arquivo", "anotacoes.txt", "text/plain", "oi".getBytes());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.salvar(texto));
        assertEquals("Formato não suportado. Envie PDF, PNG ou JPG.", erro.getMessage());
    }
}

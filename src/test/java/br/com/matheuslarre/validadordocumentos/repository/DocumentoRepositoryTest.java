package br.com.matheuslarre.validadordocumentos.repository;

import br.com.matheuslarre.validadordocumentos.model.Documento;
import br.com.matheuslarre.validadordocumentos.model.TipoDocumento;
import br.com.matheuslarre.validadordocumentos.model.RegraAplicada;
import br.com.matheuslarre.validadordocumentos.validacao.StatusValidacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Teste de integração: Spring + JPA + H2 em memória (perfil "test"). */
@SpringBootTest
@ActiveProfiles("test")
class DocumentoRepositoryTest {

    @Autowired
    private DocumentoRepository repository;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    void deveSalvarERecuperarDocumentoComChecklist() {
        Documento doc = new Documento("conta.png", "uuid.png", TipoDocumento.COMPROVANTE_RESIDENCIA,
                LocalDateTime.of(2026, 11, 24, 20, 0));
        doc.setNome("MARIA TESTE");
        doc.setDataEmissao(LocalDate.of(2026, 11, 10));
        doc.setStatus(StatusValidacao.REPROVADO);
        doc.setRegras(List.of(
                new RegraAplicada("Nome identificado", true, "MARIA TESTE"),
                new RegraAplicada("CEP válido", false, "CEP não encontrado no documento")));

        Documento salvo = repository.save(doc);
        Documento lido = repository.findById(salvo.getId()).orElseThrow();

        assertNotNull(lido.getId(), "o banco deve gerar o id (UUID)");
        assertEquals("MARIA TESTE", lido.getNome());
        assertEquals(StatusValidacao.REPROVADO, lido.getStatus());
        assertEquals(2, lido.getRegras().size());
        assertEquals("CEP válido", lido.getRegras().get(1).getNome(), "a ordem do checklist deve ser mantida");
        assertFalse(lido.getRegras().get(1).isAprovada());
    }

    @Test
    void deveListarHistoricoDoMaisRecenteParaOMaisAntigo() {
        repository.save(new Documento("antigo.png", "a.png", TipoDocumento.COMPROVANTE_RESIDENCIA, LocalDateTime.of(2026, 11, 1, 9, 0)));
        repository.save(new Documento("novo.png", "b.png", TipoDocumento.COMPROVANTE_RESIDENCIA, LocalDateTime.of(2026, 11, 24, 9, 0)));

        List<Documento> historico = repository.findAllByOrderByCriadoEmDesc();

        assertEquals("novo.png", historico.get(0).getNomeArquivoOriginal());
        assertEquals("antigo.png", historico.get(1).getNomeArquivoOriginal());
    }
}

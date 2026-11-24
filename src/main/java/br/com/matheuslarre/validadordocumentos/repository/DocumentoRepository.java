package br.com.matheuslarre.validadordocumentos.repository;

import br.com.matheuslarre.validadordocumentos.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA: só a interface. O Spring gera a implementação em tempo de execução
 * (save, findById, findAll...) e monta a consulta a partir do NOME do método abaixo.
 */
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    /** Histórico: do mais recente para o mais antigo. */
    List<Documento> findAllByOrderByCriadoEmDesc();
}

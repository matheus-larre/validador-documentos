package br.com.matheuslarre.validadordocumentos.service;

import br.com.matheuslarre.validadordocumentos.model.Documento;
import br.com.matheuslarre.validadordocumentos.repository.DocumentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas de documentos já processados (usado pelas telas e pela API REST). */
@Service
public class ConsultaDocumentoService {

    private final DocumentoRepository documentoRepository;

    public ConsultaDocumentoService(DocumentoRepository documentoRepository) {
        this.documentoRepository = documentoRepository;
    }

    /** Histórico completo, do mais recente para o mais antigo. */
    public List<Documento> listarHistorico() {
        return documentoRepository.findAllByOrderByCriadoEmDesc();
    }

    /** Busca por id; aceita texto para tratar com calma um id malformado na URL (vira "não encontrado"). */
    public Optional<Documento> buscar(String id) {
        try {
            return documentoRepository.findById(UUID.fromString(id));
        } catch (IllegalArgumentException idInvalido) {
            return Optional.empty();
        }
    }
}

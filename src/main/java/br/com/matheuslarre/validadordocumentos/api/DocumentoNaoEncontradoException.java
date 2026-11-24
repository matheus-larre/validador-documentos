package br.com.matheuslarre.validadordocumentos.api;

/** Lançada quando o id pedido na API não existe (vira HTTP 404). */
public class DocumentoNaoEncontradoException extends RuntimeException {

    public DocumentoNaoEncontradoException(String id) {
        super("Documento " + id + " não encontrado.");
    }
}

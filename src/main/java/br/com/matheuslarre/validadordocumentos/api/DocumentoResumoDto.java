package br.com.matheuslarre.validadordocumentos.api;

import br.com.matheuslarre.validadordocumentos.model.Documento;
import br.com.matheuslarre.validadordocumentos.validacao.StatusValidacao;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) da listagem: só o necessário para o histórico.
 * A API nunca devolve a entidade JPA direto: assim o formato do JSON não muda
 * quando o banco muda, e campos internos não vazam por acidente.
 */
public record DocumentoResumoDto(
        UUID id,
        LocalDateTime criadoEm,
        String arquivo,
        String tipo,
        String titular,
        StatusValidacao status
) {

    public static DocumentoResumoDto de(Documento documento) {
        return new DocumentoResumoDto(documento.getId(), documento.getCriadoEm(),
                documento.getNomeArquivoOriginal(), documento.getTipo(), documento.getNome(), documento.getStatus());
    }
}

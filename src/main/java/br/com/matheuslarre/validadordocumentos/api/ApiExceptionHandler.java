package br.com.matheuslarre.validadordocumentos.api;

import br.com.matheuslarre.validadordocumentos.ia.ExtracaoException;
import br.com.matheuslarre.validadordocumentos.service.OcrException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Erros da API em JSON padronizado (ProblemDetail, RFC 9457): {type, title, status, detail, instance}.
 * Vale só para o DocumentoApiController: as telas continuam com seus próprios tratamentos.
 * Herdar de ResponseEntityExceptionHandler faz os erros padrão do Spring
 * (ex.: parâmetro obrigatório ausente) também saírem nesse formato.
 */
@RestControllerAdvice(assignableTypes = DocumentoApiController.class)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DocumentoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(DocumentoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Documento não encontrado", e.getMessage());
    }

    /** Arquivo vazio ou formato não suportado. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail requisicaoInvalida(IllegalArgumentException e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage());
    }

    /** O arquivo chegou, mas não foi possível lê-lo. */
    @ExceptionHandler(OcrException.class)
    public ProblemDetail falhaNoOcr(OcrException e) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Documento não processável", e.getMessage());
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}

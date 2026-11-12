package br.com.matheuslarre.validadordocumentos.ia;

// Erro na etapa de extração com IA (chave ausente, falha na API ou resposta inválida)
public class ExtracaoException extends RuntimeException {

    public ExtracaoException(String mensagem) {
        super(mensagem);
    }

    public ExtracaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

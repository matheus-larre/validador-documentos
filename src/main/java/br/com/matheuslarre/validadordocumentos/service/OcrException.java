package br.com.matheuslarre.validadordocumentos.service;

// Exceção própria do OCR: deixa claro, para quem lê o código, de onde veio o problema
public class OcrException extends RuntimeException {

    public OcrException(String mensagem) {
        super(mensagem);
    }

    public OcrException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

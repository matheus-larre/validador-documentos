package br.com.matheuslarre.validadordocumentos.ia;

import java.io.Serializable;

/**
 * Tipo comum aos dados que a IA extrai de cada tipo de documento.
 * "sealed" (Java 17): só as classes listadas em "permits" podem implementar esta interface.
 * O compilador sabe que não existe um terceiro tipo "surpresa" de dados extraídos.
 */
public sealed interface DadosExtraidos extends Serializable permits DadosComprovante, DadosIdentificacao {
}

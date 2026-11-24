package br.com.matheuslarre.validadordocumentos.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

/**
 * Dados estruturados de um comprovante de residência, extraídos pela IA.
 * Um "record" é uma classe imutável só de dados: o Java gera construtor, getters, equals e toString.
 * Qualquer campo pode vir null quando a IA não o encontra no texto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DadosComprovante(
        String nome,
        String cpf,
        String endereco,
        String cep,
        String cidade,
        String estado,
        LocalDate dataEmissao
) implements DadosExtraidos {
}

package br.com.matheuslarre.validadordocumentos.service;

import br.com.matheuslarre.validadordocumentos.ia.DadosExtraidos;
import br.com.matheuslarre.validadordocumentos.validacao.ResultadoValidacao;

import java.util.UUID;

/**
 * O que o processamento devolve para quem chamou (tela ou API).
 * dados/validacao ficam nulos quando a IA falha; nesse caso erroIa explica o motivo.
 */
public record ResultadoProcessamento(
        UUID documentoId,
        String nomeArquivoSalvo,
        String textoOcr,
        DadosExtraidos dados,
        ResultadoValidacao validacao,
        String erroIa
) {
}

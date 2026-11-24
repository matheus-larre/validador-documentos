package br.com.matheuslarre.validadordocumentos.validacao;

import java.io.Serializable;
import java.util.List;

/**
 * Resultado completo da validação: o status final e o checklist de regras.
 * É Serializable porque vai para a tela via flash attribute (que fica guardado na sessão).
 */
public record ResultadoValidacao(StatusValidacao status, List<RegraResultado> regras) implements Serializable {

    /** Regra de decisão: APROVADO só se TODAS as regras passarem. */
    public static ResultadoValidacao de(List<RegraResultado> regras) {
        boolean todasPassaram = regras.stream().allMatch(RegraResultado::aprovada);
        return new ResultadoValidacao(todasPassaram ? StatusValidacao.APROVADO : StatusValidacao.REPROVADO,
                List.copyOf(regras));
    }

    public boolean aprovado() {
        return status == StatusValidacao.APROVADO;
    }
}

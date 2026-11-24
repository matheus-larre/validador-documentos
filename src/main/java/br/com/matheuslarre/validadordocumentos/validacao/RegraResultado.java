package br.com.matheuslarre.validadordocumentos.validacao;

import java.io.Serializable;

/**
 * Resultado de UMA regra: o nome que aparece no checklist, se passou e o motivo (explica o ✓ ou o ✗).
 */
public record RegraResultado(String nome, boolean aprovada, String motivo) implements Serializable {

    public static RegraResultado ok(String nome, String motivo) {
        return new RegraResultado(nome, true, motivo);
    }

    public static RegraResultado falha(String nome, String motivo) {
        return new RegraResultado(nome, false, motivo);
    }
}

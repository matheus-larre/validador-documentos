package br.com.matheuslarre.validadordocumentos.model;

/**
 * Tipos de documento aceitos. Cada tipo tem o seu prompt para a IA e o seu conjunto de regras.
 * Gravado no banco pelo nome (ex.: "COMPROVANTE_RESIDENCIA").
 */
public enum TipoDocumento {

    COMPROVANTE_RESIDENCIA("Comprovante de residência"),
    DOCUMENTO_IDENTIFICACAO("RG ou CNH");

    private final String descricao;

    TipoDocumento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Converte o valor recebido do formulário/API; valor desconhecido vira erro de requisição (400). */
    public static TipoDocumento de(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Informe o tipo de documento.");
        }
        try {
            return valueOf(valor.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de documento não suportado: " + valor + ".");
        }
    }
}

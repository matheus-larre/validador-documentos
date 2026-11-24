package br.com.matheuslarre.validadordocumentos.model;

import br.com.matheuslarre.validadordocumentos.validacao.RegraResultado;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Uma linha do checklist, gravada na tabela filha "documento_regras".
 * @Embeddable = não tem id próprio: só existe "dentro" de um Documento.
 */
@Embeddable
public class RegraAplicada {

    private String nome;
    private boolean aprovada;

    @Column(length = 500)
    private String motivo;

    protected RegraAplicada() {
        // exigido pelo JPA
    }

    public RegraAplicada(String nome, boolean aprovada, String motivo) {
        this.nome = nome;
        this.aprovada = aprovada;
        this.motivo = motivo;
    }

    public static RegraAplicada de(RegraResultado regra) {
        return new RegraAplicada(regra.nome(), regra.aprovada(), regra.motivo());
    }

    public String getNome() { return nome; }
    public boolean isAprovada() { return aprovada; }
    public String getMotivo() { return motivo; }
}

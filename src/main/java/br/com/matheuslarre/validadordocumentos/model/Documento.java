package br.com.matheuslarre.validadordocumentos.model;

import br.com.matheuslarre.validadordocumentos.validacao.StatusValidacao;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA: cada documento processado vira uma linha na tabela "documento".
 * Guarda o arquivo, o texto do OCR, os dados extraídos pela IA e o resultado da validação.
 */
@Entity
@Table(name = "documento")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // id aleatório: não revela quantos documentos existem
    private UUID id;

    @Column(nullable = false)
    private String nomeArquivoOriginal;

    @Column(nullable = false)
    private String nomeArquivoSalvo;

    @Column(nullable = false)
    private String tipo;

    @Column(columnDefinition = "TEXT")
    private String textoOcr;

    // Dados extraídos pela IA (podem ficar nulos se a IA não encontrar)
    private String nome;
    private String cpf;
    private String endereco;
    private String cep;
    private String cidade;
    private String estado;
    private LocalDate dataEmissao;

    // Nulo quando não foi possível validar (ex.: IA fora do ar)
    @Enumerated(EnumType.STRING) // grava "APROVADO"/"REPROVADO", não 0/1: legível e seguro se o enum mudar
    private StatusValidacao status;

    @Column(length = 500)
    private String erroProcessamento;

    // Checklist das regras numa tabela filha (documento_regras), na ordem em que foram aplicadas
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "documento_regras", joinColumns = @JoinColumn(name = "documento_id"))
    @OrderColumn(name = "ordem")
    private List<RegraAplicada> regras = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    protected Documento() {
        // exigido pelo JPA
    }

    public Documento(String nomeArquivoOriginal, String nomeArquivoSalvo, String tipo, LocalDateTime criadoEm) {
        this.nomeArquivoOriginal = nomeArquivoOriginal;
        this.nomeArquivoSalvo = nomeArquivoSalvo;
        this.tipo = tipo;
        this.criadoEm = criadoEm;
    }

    // ===== Getters e setters =====

    public UUID getId() { return id; }
    public String getNomeArquivoOriginal() { return nomeArquivoOriginal; }
    public String getNomeArquivoSalvo() { return nomeArquivoSalvo; }
    public String getTipo() { return tipo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public String getTextoOcr() { return textoOcr; }
    public void setTextoOcr(String textoOcr) { this.textoOcr = textoOcr; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }

    public StatusValidacao getStatus() { return status; }
    public void setStatus(StatusValidacao status) { this.status = status; }

    public String getErroProcessamento() { return erroProcessamento; }
    public void setErroProcessamento(String erroProcessamento) { this.erroProcessamento = erroProcessamento; }

    public List<RegraAplicada> getRegras() { return regras; }
    public void setRegras(List<RegraAplicada> regras) { this.regras = new ArrayList<>(regras); }
}

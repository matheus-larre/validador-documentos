package br.com.matheuslarre.validadordocumentos.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

/**
 * Dados de um documento de identificação (RG, CIN ou CNH), extraídos pela IA.
 * tipoIdentificado: "RG" ou "CNH", conforme a IA reconheceu no texto.
 * dataValidade: a CNH tem; o RG normalmente não tem (fica null).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DadosIdentificacao(
        String nome,
        String cpf,
        String numeroDocumento,
        String tipoIdentificado,
        LocalDate dataNascimento,
        LocalDate dataValidade
) implements DadosExtraidos {

    public boolean ehCnh() {
        return tipoIdentificado != null && tipoIdentificado.strip().equalsIgnoreCase("CNH");
    }
}

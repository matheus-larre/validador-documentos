package br.com.matheuslarre.validadordocumentos.validacao;

import br.com.matheuslarre.validadordocumentos.ia.DadosComprovante;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Regras de negócio do comprovante de residência.
 * Cada regra devolve um RegraResultado; o documento só é aprovado se todas passarem.
 */
@Service
public class ValidacaoService {

    static final int MIN_CARACTERES_LEGIVEL = 30;
    static final int MAX_DIAS_EMISSAO = 90;

    private static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
            "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Clock clock;

    public ValidacaoService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Aplica todas as regras.
     * @param textoOcr texto lido pelo Tesseract
     * @param dados    dados extraídos pela IA (pode ser null se o documento estava ilegível)
     */
    public ResultadoValidacao validar(String textoOcr, DadosComprovante dados) {
        DadosComprovante d = dados != null ? dados : new DadosComprovante(null, null, null, null, null, null, null);
        return ResultadoValidacao.de(List.of(
                documentoLegivel(textoOcr),
                nomeIdentificado(d.nome()),
                cpfValido(d.cpf()),
                enderecoIdentificado(d.endereco()),
                cepValido(d.cep()),
                estadoValido(d.estado()),
                emissaoRecente(d.dataEmissao())
        ));
    }

    // ===== Regras =====

    RegraResultado documentoLegivel(String textoOcr) {
        String regra = "Documento legível";
        int caracteres = textoOcr == null ? 0 : textoOcr.replaceAll("\\s", "").length();
        return caracteres >= MIN_CARACTERES_LEGIVEL
                ? RegraResultado.ok(regra, "Texto reconhecido pelo OCR (" + caracteres + " caracteres)")
                : RegraResultado.falha(regra, "Pouco ou nenhum texto reconhecido: documento ilegível");
    }

    RegraResultado nomeIdentificado(String nome) {
        String regra = "Nome identificado";
        return temTexto(nome)
                ? RegraResultado.ok(regra, nome.strip())
                : RegraResultado.falha(regra, "Nome do titular não encontrado no documento");
    }

    RegraResultado cpfValido(String cpf) {
        String regra = "CPF válido";
        if (!temTexto(cpf)) {
            return RegraResultado.falha(regra, "CPF não encontrado no documento");
        }
        return cpfComDigitosCorretos(cpf)
                ? RegraResultado.ok(regra, "Dígitos verificadores conferem")
                : RegraResultado.falha(regra, "CPF " + cpf.strip() + " é inválido (dígitos verificadores não conferem)");
    }

    RegraResultado enderecoIdentificado(String endereco) {
        String regra = "Endereço identificado";
        return temTexto(endereco)
                ? RegraResultado.ok(regra, endereco.strip())
                : RegraResultado.falha(regra, "Endereço não encontrado no documento");
    }

    RegraResultado cepValido(String cep) {
        String regra = "CEP válido";
        if (!temTexto(cep)) {
            return RegraResultado.falha(regra, "CEP não encontrado no documento");
        }
        return somenteDigitos(cep).length() == 8
                ? RegraResultado.ok(regra, cep.strip())
                : RegraResultado.falha(regra, "CEP " + cep.strip() + " não tem 8 dígitos");
    }

    RegraResultado estadoValido(String estado) {
        String regra = "Estado válido";
        if (!temTexto(estado)) {
            return RegraResultado.falha(regra, "Estado (UF) não encontrado no documento");
        }
        String uf = estado.strip().toUpperCase(Locale.ROOT);
        return UFS.contains(uf)
                ? RegraResultado.ok(regra, uf)
                : RegraResultado.falha(regra, "\"" + estado.strip() + "\" não é uma UF brasileira");
    }

    RegraResultado emissaoRecente(LocalDate dataEmissao) {
        String regra = "Emissão nos últimos " + MAX_DIAS_EMISSAO + " dias";
        if (dataEmissao == null) {
            return RegraResultado.falha(regra, "Data de emissão não encontrada no documento");
        }
        LocalDate hoje = LocalDate.now(clock);
        if (dataEmissao.isAfter(hoje)) {
            return RegraResultado.falha(regra, "Data de emissão " + dataEmissao.format(DATA_BR) + " está no futuro");
        }
        long dias = ChronoUnit.DAYS.between(dataEmissao, hoje);
        return dias <= MAX_DIAS_EMISSAO
                ? RegraResultado.ok(regra, "Emitido em " + dataEmissao.format(DATA_BR) + " (há " + dias + " dias)")
                : RegraResultado.falha(regra, "Emitido em " + dataEmissao.format(DATA_BR) + ", há " + dias
                        + " dias (limite: " + MAX_DIAS_EMISSAO + ")");
    }

    // ===== Algoritmo do CPF (módulo 11) =====

    /**
     * Um CPF tem 9 dígitos "base" + 2 dígitos verificadores (DV), calculados a partir da base:
     *  1º DV: multiplica os 9 primeiros dígitos pelos pesos 10, 9, ..., 2 e soma.
     *  2º DV: multiplica os 10 primeiros (base + 1º DV) pelos pesos 11, 10, ..., 2 e soma.
     *  Em cada um: resto = soma % 11; se resto < 2 o DV é 0, senão é 11 - resto.
     * Sequências repetidas (111.111.111-11) passam na conta, mas são inválidas por definição.
     */
    static boolean cpfComDigitosCorretos(String cpf) {
        String digitos = somenteDigitos(cpf);
        if (digitos.length() != 11 || digitos.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(digitos, 9) == digitos.charAt(9) - '0'
                && digitoVerificador(digitos, 10) == digitos.charAt(10) - '0';
    }

    /** Calcula o DV usando os "quantidade" primeiros dígitos, com pesos de (quantidade + 1) até 2. */
    private static int digitoVerificador(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    // ===== Auxiliares =====

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String somenteDigitos(String valor) {
        return valor.replaceAll("\\D", "");
    }
}

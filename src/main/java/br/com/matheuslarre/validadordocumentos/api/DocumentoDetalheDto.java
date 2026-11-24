package br.com.matheuslarre.validadordocumentos.api;

import br.com.matheuslarre.validadordocumentos.ia.DadosComprovante;
import br.com.matheuslarre.validadordocumentos.ia.DadosExtraidos;
import br.com.matheuslarre.validadordocumentos.ia.DadosIdentificacao;
import br.com.matheuslarre.validadordocumentos.model.Documento;
import br.com.matheuslarre.validadordocumentos.model.TipoDocumento;
import br.com.matheuslarre.validadordocumentos.validacao.RegraResultado;
import br.com.matheuslarre.validadordocumentos.validacao.StatusValidacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO do detalhe: resultado completo de um documento (status, checklist, dados da IA e texto do OCR).
 * "dados" traz os campos do tipo de documento: endereço/CEP no comprovante; número/nascimento/validade no RG/CNH.
 */
public record DocumentoDetalheDto(
        UUID id,
        LocalDateTime criadoEm,
        String arquivo,
        TipoDocumento tipo,
        StatusValidacao status,
        String erroProcessamento,
        List<RegraResultado> regras,
        DadosExtraidos dados,
        String textoOcr
) {

    public static DocumentoDetalheDto de(Documento d) {
        List<RegraResultado> regras = d.getRegras().stream()
                .map(r -> new RegraResultado(r.getNome(), r.isAprovada(), r.getMotivo()))
                .toList();
        return new DocumentoDetalheDto(d.getId(), d.getCriadoEm(), d.getNomeArquivoOriginal(), d.getTipo(),
                d.getStatus(), d.getErroProcessamento(), regras, dadosDoTipo(d), d.getTextoOcr());
    }

    private static DadosExtraidos dadosDoTipo(Documento d) {
        return switch (d.getTipo()) {
            case COMPROVANTE_RESIDENCIA -> new DadosComprovante(d.getNome(), d.getCpf(), d.getEndereco(),
                    d.getCep(), d.getCidade(), d.getEstado(), d.getDataEmissao());
            case DOCUMENTO_IDENTIFICACAO -> new DadosIdentificacao(d.getNome(), d.getCpf(), d.getNumeroDocumento(),
                    d.getTipoIdentificado(), d.getDataNascimento(), d.getDataValidade());
        };
    }
}

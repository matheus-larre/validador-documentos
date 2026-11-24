package br.com.matheuslarre.validadordocumentos.api;

import br.com.matheuslarre.validadordocumentos.ia.DadosComprovante;
import br.com.matheuslarre.validadordocumentos.model.Documento;
import br.com.matheuslarre.validadordocumentos.validacao.RegraResultado;
import br.com.matheuslarre.validadordocumentos.validacao.StatusValidacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** DTO do detalhe: resultado completo de um documento (status, checklist, dados da IA e texto do OCR). */
public record DocumentoDetalheDto(
        UUID id,
        LocalDateTime criadoEm,
        String arquivo,
        String tipo,
        StatusValidacao status,
        String erroProcessamento,
        List<RegraResultado> regras,
        DadosComprovante dados,
        String textoOcr
) {

    public static DocumentoDetalheDto de(Documento d) {
        List<RegraResultado> regras = d.getRegras().stream()
                .map(r -> new RegraResultado(r.getNome(), r.isAprovada(), r.getMotivo()))
                .toList();
        DadosComprovante dados = new DadosComprovante(d.getNome(), d.getCpf(), d.getEndereco(), d.getCep(),
                d.getCidade(), d.getEstado(), d.getDataEmissao());
        return new DocumentoDetalheDto(d.getId(), d.getCriadoEm(), d.getNomeArquivoOriginal(), d.getTipo(),
                d.getStatus(), d.getErroProcessamento(), regras, dados, d.getTextoOcr());
    }
}

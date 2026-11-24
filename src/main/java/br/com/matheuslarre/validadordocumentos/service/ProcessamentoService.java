package br.com.matheuslarre.validadordocumentos.service;

import br.com.matheuslarre.validadordocumentos.ia.DadosComprovante;
import br.com.matheuslarre.validadordocumentos.ia.DadosExtraidos;
import br.com.matheuslarre.validadordocumentos.ia.DadosIdentificacao;
import br.com.matheuslarre.validadordocumentos.ia.ExtracaoException;
import br.com.matheuslarre.validadordocumentos.ia.GeminiService;
import br.com.matheuslarre.validadordocumentos.model.Documento;
import br.com.matheuslarre.validadordocumentos.model.RegraAplicada;
import br.com.matheuslarre.validadordocumentos.model.TipoDocumento;
import br.com.matheuslarre.validadordocumentos.repository.DocumentoRepository;
import br.com.matheuslarre.validadordocumentos.validacao.ResultadoValidacao;
import br.com.matheuslarre.validadordocumentos.validacao.ValidacaoService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Orquestra o fluxo completo de um documento:
 * salvar arquivo → OCR → IA → regras de validação → gravar no banco.
 * Tela e API REST chamam este mesmo serviço, então a regra fica num lugar só.
 */
@Service
public class ProcessamentoService {

    private final ArmazenamentoService armazenamentoService;
    private final OcrService ocrService;
    private final GeminiService geminiService;
    private final ValidacaoService validacaoService;
    private final DocumentoRepository documentoRepository;
    private final Clock clock;

    public ProcessamentoService(ArmazenamentoService armazenamentoService,
                                OcrService ocrService,
                                GeminiService geminiService,
                                ValidacaoService validacaoService,
                                DocumentoRepository documentoRepository,
                                Clock clock) {
        this.armazenamentoService = armazenamentoService;
        this.ocrService = ocrService;
        this.geminiService = geminiService;
        this.validacaoService = validacaoService;
        this.documentoRepository = documentoRepository;
        this.clock = clock;
    }

    /**
     * Processa o arquivo enviado.
     * Erros de arquivo (tipo inválido, vazio) e de OCR sobem como exceção: nada é gravado.
     * Erro da IA não interrompe: o documento é gravado sem status, com o motivo do erro.
     */
    public ResultadoProcessamento processar(String tipoInformado, MultipartFile arquivo) {
        TipoDocumento tipo = TipoDocumento.de(tipoInformado);  // tipo inválido: nem salva o arquivo
        Path salvo = armazenamentoService.salvar(arquivo);     // 1. upload
        String texto = ocrService.extrairTexto(salvo);          // 2. OCR

        DadosExtraidos dados = null;
        ResultadoValidacao validacao = null;
        String erroIa = null;

        if (texto.isBlank()) {
            // Sem texto não há o que a IA interpretar: reprovado como ilegível
            validacao = validar(tipo, texto, null);
        } else {
            try {
                dados = extrairComIa(tipo, texto);           // 3. IA
                validacao = validar(tipo, texto, dados);     // 4. regras
            } catch (ExtracaoException e) {
                // Falha nossa (IA fora do ar) não reprova o documento: fica registrada para nova análise
                erroIa = e.getMessage();
            }
        }

        Documento documento = montarDocumento(tipo, arquivo, salvo, texto, dados, validacao, erroIa);
        Documento gravado = documentoRepository.save(documento);   // 5. banco

        return new ResultadoProcessamento(gravado.getId(), salvo.getFileName().toString(),
                texto, dados, validacao, erroIa);
    }

    // Cada tipo de documento tem o seu prompt na IA...
    private DadosExtraidos extrairComIa(TipoDocumento tipo, String texto) {
        return switch (tipo) {
            case COMPROVANTE_RESIDENCIA -> geminiService.extrairDados(texto);
            case DOCUMENTO_IDENTIFICACAO -> geminiService.extrairIdentificacao(texto);
        };
    }

    // ...e o seu conjunto de regras. O "switch" sobre o enum obriga a tratar todos os tipos.
    private ResultadoValidacao validar(TipoDocumento tipo, String texto, DadosExtraidos dados) {
        return switch (tipo) {
            case COMPROVANTE_RESIDENCIA -> validacaoService.validar(texto, (DadosComprovante) dados);
            case DOCUMENTO_IDENTIFICACAO -> validacaoService.validarIdentificacao(texto, (DadosIdentificacao) dados);
        };
    }

    private Documento montarDocumento(TipoDocumento tipo, MultipartFile arquivo, Path salvo, String texto,
                                      DadosExtraidos dados, ResultadoValidacao validacao, String erroIa) {
        String nomeOriginal = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "(sem nome)";
        Documento documento = new Documento(nomeOriginal, salvo.getFileName().toString(), tipo,
                LocalDateTime.now(clock));
        documento.setTextoOcr(texto);

        // "instanceof com variável" (Java 16+): testa o tipo e já entrega a variável convertida
        if (dados instanceof DadosComprovante c) {
            documento.setNome(c.nome());
            documento.setCpf(c.cpf());
            documento.setEndereco(c.endereco());
            documento.setCep(c.cep());
            documento.setCidade(c.cidade());
            documento.setEstado(c.estado());
            documento.setDataEmissao(c.dataEmissao());
        } else if (dados instanceof DadosIdentificacao i) {
            documento.setNome(i.nome());
            documento.setCpf(i.cpf());
            documento.setNumeroDocumento(i.numeroDocumento());
            documento.setTipoIdentificado(i.tipoIdentificado());
            documento.setDataNascimento(i.dataNascimento());
            documento.setDataValidade(i.dataValidade());
        }
        if (validacao != null) {
            documento.setStatus(validacao.status());
            documento.setRegras(validacao.regras().stream().map(RegraAplicada::de).toList());
        }
        documento.setErroProcessamento(erroIa);
        return documento;
    }
}

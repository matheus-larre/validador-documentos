package br.com.matheuslarre.validadordocumentos.service;

import br.com.matheuslarre.validadordocumentos.ia.DadosComprovante;
import br.com.matheuslarre.validadordocumentos.ia.ExtracaoException;
import br.com.matheuslarre.validadordocumentos.ia.GeminiService;
import br.com.matheuslarre.validadordocumentos.model.Documento;
import br.com.matheuslarre.validadordocumentos.model.RegraAplicada;
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
    public ResultadoProcessamento processar(String tipo, MultipartFile arquivo) {
        Path salvo = armazenamentoService.salvar(arquivo);   // 1. upload
        String texto = ocrService.extrairTexto(salvo);        // 2. OCR

        DadosComprovante dados = null;
        ResultadoValidacao validacao = null;
        String erroIa = null;

        if (texto.isBlank()) {
            // Sem texto não há o que a IA interpretar: reprovado como ilegível
            validacao = validacaoService.validar(texto, null);
        } else {
            try {
                dados = geminiService.extrairDados(texto);             // 3. IA
                validacao = validacaoService.validar(texto, dados);    // 4. regras
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

    private Documento montarDocumento(String tipo, MultipartFile arquivo, Path salvo, String texto,
                                      DadosComprovante dados, ResultadoValidacao validacao, String erroIa) {
        String nomeOriginal = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "(sem nome)";
        Documento documento = new Documento(nomeOriginal, salvo.getFileName().toString(), tipo,
                LocalDateTime.now(clock));
        documento.setTextoOcr(texto);

        if (dados != null) {
            documento.setNome(dados.nome());
            documento.setCpf(dados.cpf());
            documento.setEndereco(dados.endereco());
            documento.setCep(dados.cep());
            documento.setCidade(dados.cidade());
            documento.setEstado(dados.estado());
            documento.setDataEmissao(dados.dataEmissao());
        }
        if (validacao != null) {
            documento.setStatus(validacao.status());
            documento.setRegras(validacao.regras().stream().map(RegraAplicada::de).toList());
        }
        documento.setErroProcessamento(erroIa);
        return documento;
    }
}

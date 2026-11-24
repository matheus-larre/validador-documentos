package br.com.matheuslarre.validadordocumentos.controller;

import br.com.matheuslarre.validadordocumentos.ia.DadosComprovante;
import br.com.matheuslarre.validadordocumentos.ia.ExtracaoException;
import br.com.matheuslarre.validadordocumentos.ia.GeminiService;
import br.com.matheuslarre.validadordocumentos.service.ArmazenamentoService;
import br.com.matheuslarre.validadordocumentos.service.OcrException;
import br.com.matheuslarre.validadordocumentos.service.OcrService;
import br.com.matheuslarre.validadordocumentos.validacao.ValidacaoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;

@Controller
public class DocumentoController {

    private final ArmazenamentoService armazenamentoService;
    private final OcrService ocrService;
    private final GeminiService geminiService;
    private final ValidacaoService validacaoService;

    // Injeção de dependência pelo construtor: o Spring entrega os Services prontos
    public DocumentoController(ArmazenamentoService armazenamentoService,
                               OcrService ocrService,
                               GeminiService geminiService,
                               ValidacaoService validacaoService) {
        this.armazenamentoService = armazenamentoService;
        this.ocrService = ocrService;
        this.geminiService = geminiService;
        this.validacaoService = validacaoService;
    }

    @GetMapping("/")
    public String telaUpload() {
        return "upload";
    }

    @PostMapping("/documentos")
    public String receberDocumento(@RequestParam("tipo") String tipo,
                                   @RequestParam("arquivo") MultipartFile arquivo,
                                   RedirectAttributes redirect) {
        try {
            Path salvo = armazenamentoService.salvar(arquivo);   // 1. upload
            String texto = ocrService.extrairTexto(salvo);        // 2. OCR

            redirect.addFlashAttribute("sucesso",
                    "Arquivo \"" + arquivo.getOriginalFilename() + "\" recebido como " + tipo
                    + ". Salvo como " + salvo.getFileName() + ".");
            redirect.addFlashAttribute("textoExtraido",
                    texto.isBlank() ? "(nenhum texto reconhecido)" : texto);

            if (texto.isBlank()) {
                // Sem texto não há o que a IA interpretar: o documento é reprovado como ilegível
                redirect.addFlashAttribute("resultado", validacaoService.validar(texto, null));
            } else {
                try {
                    DadosComprovante dados = geminiService.extrairDados(texto);                      // 3. IA
                    redirect.addFlashAttribute("dados", dados);
                    redirect.addFlashAttribute("resultado", validacaoService.validar(texto, dados)); // 4. regras
                } catch (ExtracaoException e) {
                    // Falha nossa (IA fora do ar) não reprova o documento: só avisa e mostra o OCR
                    redirect.addFlashAttribute("erro",
                            "O texto foi lido, mas a IA não conseguiu extrair os dados: " + e.getMessage());
                }
            }
        } catch (IllegalArgumentException | OcrException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        // Padrão PRG (Post/Redirect/Get): evita reenviar o arquivo ao apertar F5
        return "redirect:/";
    }
}

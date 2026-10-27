package br.com.matheuslarre.validadordocumentos.controller;

import br.com.matheuslarre.validadordocumentos.service.ArmazenamentoService;
import br.com.matheuslarre.validadordocumentos.service.OcrException;
import br.com.matheuslarre.validadordocumentos.service.OcrService;
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

    // Injeção de dependência pelo construtor: o Spring entrega os Services prontos
    public DocumentoController(ArmazenamentoService armazenamentoService, OcrService ocrService) {
        this.armazenamentoService = armazenamentoService;
        this.ocrService = ocrService;
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
        } catch (IllegalArgumentException | OcrException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        // Padrão PRG (Post/Redirect/Get): evita reenviar o arquivo ao apertar F5
        return "redirect:/";
    }
}

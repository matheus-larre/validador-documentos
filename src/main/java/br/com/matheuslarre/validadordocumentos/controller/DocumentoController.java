package br.com.matheuslarre.validadordocumentos.controller;

import br.com.matheuslarre.validadordocumentos.service.OcrException;
import br.com.matheuslarre.validadordocumentos.service.ProcessamentoService;
import br.com.matheuslarre.validadordocumentos.service.ResultadoProcessamento;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DocumentoController {

    private final ProcessamentoService processamentoService;

    // Injeção de dependência pelo construtor: o Spring entrega o Service pronto
    public DocumentoController(ProcessamentoService processamentoService) {
        this.processamentoService = processamentoService;
    }

    @GetMapping("/")
    public String telaUpload() {
        return "upload";
    }

    /** Controller "fino": recebe o HTTP, delega o fluxo ao serviço e decide o que mostrar na tela. */
    @PostMapping("/documentos")
    public String receberDocumento(@RequestParam("tipo") String tipo,
                                   @RequestParam("arquivo") MultipartFile arquivo,
                                   RedirectAttributes redirect) {
        try {
            ResultadoProcessamento resultado = processamentoService.processar(tipo, arquivo);

            redirect.addFlashAttribute("sucesso",
                    "Arquivo \"" + arquivo.getOriginalFilename() + "\" recebido como " + tipo
                    + ". Salvo como " + resultado.nomeArquivoSalvo() + ".");
            redirect.addFlashAttribute("textoExtraido",
                    resultado.textoOcr().isBlank() ? "(nenhum texto reconhecido)" : resultado.textoOcr());
            redirect.addFlashAttribute("dados", resultado.dados());
            redirect.addFlashAttribute("resultado", resultado.validacao());
            if (resultado.erroIa() != null) {
                redirect.addFlashAttribute("erro",
                        "O texto foi lido, mas a IA não conseguiu extrair os dados: " + resultado.erroIa());
            }
        } catch (IllegalArgumentException | OcrException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        // Padrão PRG (Post/Redirect/Get): evita reenviar o arquivo ao apertar F5
        return "redirect:/";
    }
}

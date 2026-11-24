package br.com.matheuslarre.validadordocumentos.controller;

import br.com.matheuslarre.validadordocumentos.model.Documento;
import br.com.matheuslarre.validadordocumentos.service.ConsultaDocumentoService;
import br.com.matheuslarre.validadordocumentos.service.OcrException;
import br.com.matheuslarre.validadordocumentos.service.ProcessamentoService;
import br.com.matheuslarre.validadordocumentos.service.ResultadoProcessamento;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class DocumentoController {

    private final ProcessamentoService processamentoService;
    private final ConsultaDocumentoService consultaDocumentoService;

    // Injeção de dependência pelo construtor: o Spring entrega os Services prontos
    public DocumentoController(ProcessamentoService processamentoService,
                               ConsultaDocumentoService consultaDocumentoService) {
        this.processamentoService = processamentoService;
        this.consultaDocumentoService = consultaDocumentoService;
    }

    @GetMapping("/")
    public String telaUpload() {
        return "upload";
    }

    /** Controller "fino": recebe o HTTP, delega o fluxo ao serviço e decide para qual tela ir. */
    @PostMapping("/documentos")
    public String receberDocumento(@RequestParam("tipo") String tipo,
                                   @RequestParam("arquivo") MultipartFile arquivo,
                                   RedirectAttributes redirect) {
        try {
            ResultadoProcessamento resultado = processamentoService.processar(tipo, arquivo);
            redirect.addFlashAttribute("sucesso",
                    "Arquivo \"" + arquivo.getOriginalFilename() + "\" processado e salvo no histórico.");
            // PRG (Post/Redirect/Get): o resultado agora vem do banco, então F5 ou link direto continuam funcionando
            return "redirect:/documentos/" + resultado.documentoId();
        } catch (IllegalArgumentException | OcrException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/documentos")
    public String historico(Model model) {
        model.addAttribute("documentos", consultaDocumentoService.listarHistorico());
        return "historico";
    }

    @GetMapping("/documentos/{id}")
    public String detalhe(@PathVariable String id, Model model, RedirectAttributes redirect) {
        Optional<Documento> documento = consultaDocumentoService.buscar(id);
        if (documento.isEmpty()) {
            redirect.addFlashAttribute("erro", "Documento não encontrado.");
            return "redirect:/documentos";
        }
        model.addAttribute("doc", documento.get());
        return "detalhe";
    }
}

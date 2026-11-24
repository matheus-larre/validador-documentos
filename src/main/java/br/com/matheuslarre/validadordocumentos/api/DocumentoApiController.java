package br.com.matheuslarre.validadordocumentos.api;

import br.com.matheuslarre.validadordocumentos.service.ConsultaDocumentoService;
import br.com.matheuslarre.validadordocumentos.service.ProcessamentoService;
import br.com.matheuslarre.validadordocumentos.service.ResultadoProcessamento;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST: o mesmo fluxo da tela, mas devolvendo JSON para outros sistemas.
 * @RestController = @Controller + @ResponseBody: o retorno vira JSON (não o nome de uma tela).
 */
@RestController
@RequestMapping("/api/documentos")
public class DocumentoApiController {

    private final ProcessamentoService processamentoService;
    private final ConsultaDocumentoService consultaDocumentoService;

    public DocumentoApiController(ProcessamentoService processamentoService,
                                  ConsultaDocumentoService consultaDocumentoService) {
        this.processamentoService = processamentoService;
        this.consultaDocumentoService = consultaDocumentoService;
    }

    /** Envia um documento para validação. Responde 201 Created + cabeçalho Location com o endereço do recurso. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoDetalheDto> enviar(@RequestParam("tipo") String tipo,
                                                      @RequestParam("arquivo") MultipartFile arquivo) {
        ResultadoProcessamento resultado = processamentoService.processar(tipo, arquivo);
        String id = resultado.documentoId().toString();

        URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(local).body(buscarDetalhe(id));
    }

    /** Histórico resumido, do mais recente para o mais antigo. */
    @GetMapping
    public List<DocumentoResumoDto> listar() {
        return consultaDocumentoService.listarHistorico().stream().map(DocumentoResumoDto::de).toList();
    }

    /** Detalhe de um documento, ou 404 se não existir. */
    @GetMapping("/{id}")
    public DocumentoDetalheDto detalhar(@PathVariable String id) {
        return buscarDetalhe(id);
    }

    private DocumentoDetalheDto buscarDetalhe(String id) {
        return consultaDocumentoService.buscar(id)
                .map(DocumentoDetalheDto::de)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(id));
    }
}

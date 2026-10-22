package br.com.matheuslarre.validadordocumentos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // diz ao Spring: esta classe responde requisições e devolve páginas HTML
public class DocumentoController {

    // Quando alguém acessa http://localhost:8080/ ...
    @GetMapping("/")
    public String telaUpload() {
        // ... o Spring procura o arquivo templates/upload.html e mostra no navegador
        return "upload";
    }
}

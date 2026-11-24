package br.com.matheuslarre.validadordocumentos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Página explicativa: o que a ferramenta faz, como usar e quais regras aplica. */
@Controller
public class ComoFuncionaController {

    @GetMapping("/como-funciona")
    public String comoFunciona() {
        return "como-funciona";
    }
}

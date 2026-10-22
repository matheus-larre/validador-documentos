package br.com.matheuslarre.validadordocumentos.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice // "vigia" todos os controllers e trata erros num lugar só
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String arquivoGrandeDemais(RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", "O arquivo passa de 10MB. Envie um arquivo menor.");
        return "redirect:/";
    }
}

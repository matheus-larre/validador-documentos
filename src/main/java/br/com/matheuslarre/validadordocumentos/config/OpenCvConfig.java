package br.com.matheuslarre.validadordocumentos.config;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration // o Spring cria esta classe ao iniciar a aplicação, então o OpenCV é carregado uma vez no começo
public class OpenCvConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenCvConfig.class);

    public OpenCvConfig() {
        carregarBiblioteca();
        log.info("OpenCV carregado com sucesso (versão {})", Core.VERSION);
    }

    /**
     * Carrega a biblioteca nativa do OpenCV só se ela ainda não estiver carregada.
     * Isso evita erro quando o DevTools reinicia a aplicação (a JVM continua a mesma).
     * É "static" para que os testes também possam chamar sem subir o Spring.
     */
    public static void carregarBiblioteca() {
        try {
            Core.getVersionString(); // se funcionar, a biblioteca já está carregada
        } catch (UnsatisfiedLinkError aindaNaoCarregada) {
            OpenCV.loadLocally();    // extrai a biblioteca nativa do .jar e carrega
        }
    }
}

package br.com.matheuslarre.validadordocumentos.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class OcrService {

    private final String tessdataPath;
    private final String idioma;

    public OcrService(@Value("${ocr.tessdata-path}") String tessdataPath,
                      @Value("${ocr.idioma}") String idioma) {
        this.tessdataPath = tessdataPath;
        this.idioma = idioma;
    }

    /** Lê o arquivo salvo e devolve o texto reconhecido pelo Tesseract (uma "string crua", sem significado). */
    public String extrairTexto(Path arquivo) {
        List<BufferedImage> paginas = carregarPaginas(arquivo);
        ITesseract tesseract = criarTesseract();

        StringBuilder texto = new StringBuilder();
        try {
            for (BufferedImage pagina : paginas) {
                texto.append(tesseract.doOCR(pagina)).append(System.lineSeparator());
            }
        } catch (TesseractException e) {
            throw new OcrException("Não foi possível ler o texto do documento.", e);
        }
        return texto.toString().trim();
    }

    // Uma instância nova por leitura: o objeto Tesseract não é seguro para uso simultâneo
    private ITesseract criarTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath); // onde estão os arquivos de idioma (.traineddata)
        tesseract.setLanguage(idioma);        // "por" = português
        tesseract.setVariable("user_defined_dpi", "300");
        return tesseract;
    }

    private List<BufferedImage> carregarPaginas(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        if (nome.endsWith(".pdf")) {
            throw new OcrException("Leitura de PDF ainda não implementada. Envie PNG ou JPG por enquanto.");
        }
        try {
            BufferedImage imagem = ImageIO.read(arquivo.toFile());
            if (imagem == null) {
                throw new OcrException("Não foi possível abrir a imagem enviada.");
            }
            return List.of(imagem);
        } catch (IOException e) {
            throw new OcrException("Falha ao abrir a imagem enviada.", e);
        }
    }
}

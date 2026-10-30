package br.com.matheuslarre.validadordocumentos.imagem;

import br.com.matheuslarre.validadordocumentos.config.OpenCvConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ConversorImagemTest {

    @BeforeAll
    static void carregarOpenCv() {
        OpenCvConfig.carregarBiblioteca(); // teste unitário não sobe o Spring, então carregamos manualmente
    }

    @Test
    void deveConverterImagemColoridaParaMatEVoltarSemPerderCores() {
        BufferedImage original = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        original.setRGB(5, 5, 0xFF0000); // um pixel vermelho

        Mat mat = ConversorImagem.paraMat(original);
        try {
            assertEquals(20, mat.rows(), "altura");
            assertEquals(40, mat.cols(), "largura");
            assertEquals(3, mat.channels(), "deveria ter 3 canais (BGR)");

            double[] pixel = mat.get(5, 5); // ordem BGR
            assertEquals(0, pixel[0], "azul");
            assertEquals(0, pixel[1], "verde");
            assertEquals(255, pixel[2], "vermelho");

            BufferedImage deVolta = ConversorImagem.paraBufferedImage(mat);
            assertEquals(0xFF0000, deVolta.getRGB(5, 5) & 0xFFFFFF, "a cor deveria se manter na volta");
        } finally {
            mat.release(); // Mat usa memória nativa: liberar quando não precisar mais
        }
    }

    @Test
    void deveConverterMatEmTonsDeCinzaParaImagemCinza() {
        Mat cinza = new Mat(10, 10, CvType.CV_8UC1);
        try {
            cinza.setTo(new org.opencv.core.Scalar(128)); // cinza médio
            BufferedImage imagem = ConversorImagem.paraBufferedImage(cinza);

            assertEquals(BufferedImage.TYPE_BYTE_GRAY, imagem.getType());
            assertEquals(128, imagem.getRaster().getSample(3, 3, 0));
        } finally {
            cinza.release();
        }
    }
}

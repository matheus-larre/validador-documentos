package br.com.matheuslarre.validadordocumentos.imagem;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Converte imagens entre o formato do Java (BufferedImage) e o do OpenCV (Mat).
 * O OpenCV guarda pixels coloridos na ordem BGR (azul, verde, vermelho), não RGB.
 */
public final class ConversorImagem {

    private ConversorImagem() {
        // classe utilitária: só métodos estáticos, não deve ser instanciada
    }

    /** BufferedImage (qualquer tipo) -> Mat colorida de 3 canais (BGR). */
    public static Mat paraMat(BufferedImage imagem) {
        // Redesenha a imagem num formato BGR padronizado, seja ela PNG, JPG ou página de PDF
        BufferedImage bgr = new BufferedImage(imagem.getWidth(), imagem.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D desenho = bgr.createGraphics();
        desenho.drawImage(imagem, 0, 0, null);
        desenho.dispose();

        byte[] pixels = ((DataBufferByte) bgr.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(bgr.getHeight(), bgr.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }

    /** Mat (1 canal = cinza, ou 3 canais = BGR) -> BufferedImage. */
    public static BufferedImage paraBufferedImage(Mat mat) {
        int tipo = mat.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;
        BufferedImage imagem = new BufferedImage(mat.cols(), mat.rows(), tipo);
        byte[] pixels = ((DataBufferByte) imagem.getRaster().getDataBuffer()).getData();
        mat.get(0, 0, pixels);
        return imagem;
    }
}

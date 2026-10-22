package br.com.matheuslarre.validadordocumentos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service // diz ao Spring: esta classe tem regra de negócio e pode ser injetada em outras
public class ArmazenamentoService {

    private static final Set<String> TIPOS_PERMITIDOS =
            Set.of("application/pdf", "image/png", "image/jpeg");

    private final Path pastaUploads;

    // @Value lê o valor "app.upload.dir" do application.properties
    public ArmazenamentoService(@Value("${app.upload.dir}") String pasta) throws IOException {
        this.pastaUploads = Paths.get(pasta).toAbsolutePath().normalize();
        Files.createDirectories(this.pastaUploads); // cria a pasta se não existir
    }

    public Path salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Selecione um arquivo antes de enviar.");
        }
        if (!TIPOS_PERMITIDOS.contains(arquivo.getContentType())) {
            throw new IllegalArgumentException("Formato não suportado. Envie PDF, PNG ou JPG.");
        }

        // Nome aleatório (UUID): evita sobrescrever arquivos e não expõe o nome original
        String nomeSalvo = UUID.randomUUID() + extrairExtensao(arquivo.getOriginalFilename());
        Path destino = pastaUploads.resolve(nomeSalvo);

        try (InputStream entrada = arquivo.getInputStream()) {
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar o arquivo.", e);
        }
        return destino;
    }

    private String extrairExtensao(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) return "";
        return nomeOriginal.substring(nomeOriginal.lastIndexOf('.')).toLowerCase();
    }
}

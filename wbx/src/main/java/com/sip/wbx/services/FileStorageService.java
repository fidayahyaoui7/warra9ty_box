package com.sip.wbx.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FileStorageService {

    public static final Set<String> IMAGES = Set.of("image/jpeg", "image/png");
    public static final Set<String> PDF = Set.of("application/pdf");
    private static final Map<String, String> EXT =
        Map.of("image/jpeg", ".jpg", "image/png", ".png", "application/pdf", ".pdf");

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    // Vérifie le type et la taille, renomme en UUID, renvoie le nom du fichier
    public String save(MultipartFile file, String subDir, Set<String> allowed, long maxBytes) {
        if (file == null || file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier manquant");
        String type = file.getContentType();
        if (type == null || !allowed.contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de fichier non autorisé : " + type);
        if (file.getSize() > maxBytes)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Fichier trop volumineux (max " + (maxBytes / 1024 / 1024) + " Mo)");
        try {
            Path dir = root.resolve(subDir);
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + EXT.get(type);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return name;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur d'enregistrement du fichier");
        }
    }

    // Suppression physique (RG6)
    public void delete(String subDir, String filename) {
        if (filename == null) return;
        try {
            Path file = root.resolve(subDir).resolve(filename).normalize();
            if (file.startsWith(root)) Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    public Resource load(String subDir, String filename) {
        try {
            Path file = root.resolve(subDir).resolve(filename).normalize();
            if (!file.startsWith(root) || !Files.exists(file))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable");
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable");
        }
    }
}
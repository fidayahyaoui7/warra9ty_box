package com.sip.wbx.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.sip.wbx.entities.StoredFile;
import com.sip.wbx.repositories.StoredFileRepository;

/**
 * Les fichiers téléversés sont enregistrés dans la base de données (table stored_file), donc ils survivent aux
 * redémarrages. Le dossier "uploads" ne sert plus qu'à lire les photos du catalogue de démonstration fournies
 * avec le projet.
 */
@Service
public class FileStorageService {

    public static final Set<String> IMAGES = Set.of("image/jpeg", "image/png");
    public static final Set<String> PDF = Set.of("application/pdf");
    private static final Map<String, String> EXT =
        Map.of("image/jpeg", ".jpg", "image/png", ".png", "application/pdf", ".pdf");

    private final Path root;
    private final StoredFileRepository repo;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String dir, StoredFileRepository repo)
            throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        this.repo = repo;
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
            String name = UUID.randomUUID() + EXT.get(type);
            StoredFile f = new StoredFile();
            f.setPath(subDir + "/" + name);
            f.setContentType(type);
            f.setData(file.getBytes());
            repo.save(f);
            return name;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur d'enregistrement du fichier");
        }
    }

    // Suppression (RG6)
    public void delete(String subDir, String filename) {
        if (filename == null) return;
        String key = subDir + "/" + filename;
        if (repo.existsById(key)) repo.deleteById(key);
        try {
            Path file = root.resolve(subDir).resolve(filename).normalize();
            if (file.startsWith(root)) Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    public Resource load(String subDir, String filename) {
        if (filename == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable");
        var stored = repo.findById(subDir + "/" + filename);
        if (stored.isPresent())
            return new NamedBytes(stored.get().getData(), filename);
        // photos de démonstration fournies dans le dossier uploads
        try {
            Path file = root.resolve(subDir).resolve(filename).normalize();
            if (!file.startsWith(root) || !Files.exists(file))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable");
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable");
        }
    }

    // ByteArrayResource avec un nom de fichier (pour deviner le type d'image)
    private static class NamedBytes extends ByteArrayResource {
        private final String filename;

        NamedBytes(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}

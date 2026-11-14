package com.sip.wbx.controllers;

import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.sip.wbx.services.FileStorageService;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:4200")
public class FileController {

    private final FileStorageService storage;

    public FileController(FileStorageService storage) {
        this.storage = storage;
    }

    // Photos du catalogue (publiques : une balise <img> ne peut pas envoyer de token)
    @GetMapping("/produits/{filename:.+}")
    public ResponseEntity<Resource> produitPhoto(@PathVariable String filename) {
        return image("produits", filename);
    }

    // Photos de profil (publiques, noms UUID non devinables)
    @GetMapping("/profils/{filename:.+}")
    public ResponseEntity<Resource> profilPhoto(@PathVariable String filename) {
        return image("profils", filename);
    }

    private ResponseEntity<Resource> image(String dir, String filename) {
        Resource r = storage.load(dir, filename);
        MediaType type = MediaTypeFactory.getMediaType(r).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).body(r);
    }
}

package com.sip.wbx.entities;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

/**
 * Fichier téléversé (photo de produit, photo de profil, facture PDF, justificatif...), conservé dans la base
 * de données : sur un hébergeur dont le disque est éphémère (Render gratuit), les fichiers du disque disparaissent
 * à chaque redémarrage, alors que la base de données reste.
 */
@Entity
@Table(name = "stored_file")
@Getter @Setter @NoArgsConstructor
public class StoredFile {

    @Id
    @Column(length = 255)
    private String path;                       // ex. "factures/3f2c...e1.pdf"

    @Column(nullable = false, length = 100)
    private String contentType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    private LocalDateTime createdAt = LocalDateTime.now();
}

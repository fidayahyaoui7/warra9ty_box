package com.sip.wbx.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor
public class Reclamation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateOuverture = LocalDate.now();
    @NotBlank private String motif;
    @Column(length = 1000) private String description;

    private String statut = "OUVERTE";   // OUVERTE, EN_COURS, RESOLUE, REFUSEE
    private String justificatifPdf;      // nom du fichier PDF
    @Column(length = 1000) private String reponseAdmin;   // message de l'admin lors du changement de statut (optionnel)

    @ManyToOne(optional = false)
    @JoinColumn(name = "achat_id")
    private Achat achat;
}
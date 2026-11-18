package com.sip.wbx.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class Achat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull private LocalDate dateAchat;
    @Positive private double prixPaye;
    private String magasin;
    private String numeroSerie;
    private Integer dureeGarantie;       // en mois (sinon = standard du produit)
    private LocalDate dateFinGarantie;   // calculée par le service (RG1)

    private String facturePdf;           // nom du fichier PDF
    private String photoAchat;           // nom du fichier image

    @JsonIgnore
    private Boolean rappelEnvoye = false;   // e-mail de rappel déjà envoyé pour cette garantie ?

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @OneToMany(mappedBy = "achat")
    @JsonIgnore
    private List<Reclamation> reclamations = new ArrayList<>();

    // RG2: ACTIVE (more than 30 days left), BIENTOT_EXPIREE (30 days or less), EXPIREE
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getStatutGarantie() {
        if (dateFinGarantie == null) return "INCONNU";
        LocalDate today = LocalDate.now();
        if (dateFinGarantie.isBefore(today)) return "EXPIREE";
        if (!dateFinGarantie.isAfter(today.plusDays(30))) return "BIENTOT_EXPIREE";
        return "ACTIVE";
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public long getJoursRestants() {
        if (dateFinGarantie == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), dateFinGarantie);
    }
}
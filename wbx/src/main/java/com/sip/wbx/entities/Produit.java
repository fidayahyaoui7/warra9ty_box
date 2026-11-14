package com.sip.wbx.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class Produit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank private String nom;       // ex. Galaxy S24
    @NotBlank private String marque;    // ex. Samsung
    private String modele;
    @Positive private double prix;
    @Column(length = 1000) private String description;
    @Positive private int dureeGarantieStandard;  // en mois
    private String photo;                         // nom du fichier image

    @ManyToOne(optional = false)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @OneToMany(mappedBy = "produit")
    @JsonIgnore
    private List<Achat> achats = new ArrayList<>();
}
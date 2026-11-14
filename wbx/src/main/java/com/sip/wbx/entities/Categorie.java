package com.sip.wbx.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class Categorie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String libelle;
    private String description;

    @OneToMany(mappedBy = "categorie")
    @JsonIgnore
    private List<Produit> produits = new ArrayList<>();
}
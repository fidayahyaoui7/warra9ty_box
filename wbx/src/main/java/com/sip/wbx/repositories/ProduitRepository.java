package com.sip.wbx.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sip.wbx.entities.Produit;

public interface ProduitRepository extends JpaRepository<Produit, Long> {
    List<Produit> findByNomContainingIgnoreCase(String nom);
    List<Produit> findByCategorieId(Long categorieId);
}
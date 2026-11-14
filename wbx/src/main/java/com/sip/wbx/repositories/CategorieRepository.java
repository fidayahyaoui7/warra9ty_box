package com.sip.wbx.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sip.wbx.entities.Categorie;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {
}
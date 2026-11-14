package com.sip.wbx.services;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.sip.wbx.entities.Categorie;
import com.sip.wbx.repositories.CategorieRepository;

@Service
public class CategorieService {

    private final CategorieRepository repo;

    public CategorieService(CategorieRepository repo) {
        this.repo = repo;
    }

    public List<Categorie> findAll() {
        return repo.findAll();
    }

    public Categorie findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Catégorie introuvable : " + id));
    }

    public Categorie save(Categorie c) {
        return repo.save(c);
    }

    public Categorie update(Long id, Categorie c) {
        Categorie existing = findById(id);
        existing.setLibelle(c.getLibelle());
        existing.setDescription(c.getDescription());
        return repo.save(existing);
    }

    public void delete(Long id) {
        repo.delete(findById(id));
    }
}
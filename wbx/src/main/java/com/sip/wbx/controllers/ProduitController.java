package com.sip.wbx.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.sip.wbx.entities.Produit;
import com.sip.wbx.services.ProduitService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/produits")
@CrossOrigin(origins = "http://localhost:4200")
public class ProduitController {

    private final ProduitService service;

    public ProduitController(ProduitService service) {
        this.service = service;
    }

    // GET /api/produits  ou  /api/produits?nom=galaxy  ou  /api/produits?categorieId=1
    @GetMapping
    public List<Produit> list(@RequestParam(required = false) String nom,
                              @RequestParam(required = false) Long categorieId) {
        if (nom != null) return service.search(nom);
        if (categorieId != null) return service.findByCategorie(categorieId);
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Produit get(@PathVariable Long id) {
        return service.findById(id);
    }

    // Création : le produit (JSON) + la photo (fichier), photo obligatoire
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Produit add(@Valid @RequestPart("produit") Produit p,
                       @RequestPart("photo") MultipartFile photo) {
        return service.save(p, photo);
    }

    // Modification : photo optionnelle (si absente, on garde l'ancienne)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Produit update(@PathVariable Long id,
                          @Valid @RequestPart("produit") Produit p,
                          @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return service.update(id, p, photo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
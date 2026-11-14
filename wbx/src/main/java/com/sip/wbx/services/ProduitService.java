package com.sip.wbx.services;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.sip.wbx.entities.Categorie;
import com.sip.wbx.entities.Produit;
import com.sip.wbx.repositories.ProduitRepository;

@Service
public class ProduitService {

    private static final long MAX_IMAGE = 2 * 1024 * 1024;   // 2 Mo

    private final ProduitRepository repo;
    private final CategorieService categorieService;
    private final FileStorageService storage;

    public ProduitService(ProduitRepository repo, CategorieService categorieService,
                          FileStorageService storage) {
        this.repo = repo;
        this.categorieService = categorieService;
        this.storage = storage;
    }

    public List<Produit> findAll() {
        return repo.findAll();
    }

    public List<Produit> search(String nom) {
        return repo.findByNomContainingIgnoreCase(nom);
    }

    public List<Produit> findByCategorie(Long categorieId) {
        return repo.findByCategorieId(categorieId);
    }

    public Produit findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable : " + id));
    }

    // On vérifie que la catégorie existe vraiment avant d'enregistrer
    private Categorie resolveCategorie(Produit p) {
        if (p.getCategorie() == null || p.getCategorie().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La catégorie est obligatoire");
        }
        return categorieService.findById(p.getCategorie().getId());
    }

    // Création : la photo est obligatoire
    public Produit save(Produit p, MultipartFile photo) {
        p.setCategorie(resolveCategorie(p));
        p.setPhoto(storage.save(photo, "produits", FileStorageService.IMAGES, MAX_IMAGE));
        return repo.save(p);
    }

    // Modification : la photo est optionnelle (si absente, on garde l'ancienne)
    public Produit update(Long id, Produit p, MultipartFile photo) {
        Produit existing = findById(id);
        existing.setNom(p.getNom());
        existing.setMarque(p.getMarque());
        existing.setModele(p.getModele());
        existing.setPrix(p.getPrix());
        existing.setDescription(p.getDescription());
        existing.setDureeGarantieStandard(p.getDureeGarantieStandard());
        existing.setCategorie(resolveCategorie(p));

        if (photo != null && !photo.isEmpty()) {
            String newName = storage.save(photo, "produits", FileStorageService.IMAGES, MAX_IMAGE);
            storage.delete("produits", existing.getPhoto());   // supprime l'ancienne du disque
            existing.setPhoto(newName);
        }
        return repo.save(existing);
    }

    public void delete(Long id) {
        Produit existing = findById(id);
        repo.delete(existing);                                 // erreur 409 si le produit a des achats
        storage.delete("produits", existing.getPhoto());       // fichier supprimé seulement ensuite
    }
}
package com.sip.wbx.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.sip.wbx.entities.Achat;
import com.sip.wbx.entities.Produit;
import com.sip.wbx.entities.User;
import com.sip.wbx.repositories.AchatRepository;
import com.sip.wbx.repositories.UserRepository;
import com.sip.wbx.security.CurrentUser;

@Service
public class AchatService {

    private static final long MAX_PDF = 5 * 1024 * 1024;   // 5 MB
    private static final long MAX_IMG = 2 * 1024 * 1024;   // 2 MB

    private final AchatRepository repo;
    private final UserRepository userRepo;
    private final ProduitService produitService;
    private final FileStorageService storage;

    public AchatService(AchatRepository repo, UserRepository userRepo,
                        ProduitService produitService, FileStorageService storage) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.produitService = produitService;
        this.storage = storage;
    }

    // RG5 : un client voit ses achats, l'admin voit tout
    public List<Achat> findVisible(CurrentUser me) {
        return me.admin() ? repo.findAll() : repo.findByUserId(me.id());
    }

    public List<Achat> findByUser(Long userId) {
        return repo.findByUserId(userId);
    }

    // RG5 : propriétaire ou admin
    public Achat findAccessible(Long id, CurrentUser me) {
        Achat a = repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Achat introuvable : " + id));
        if (!me.admin() && !a.getUser().getId().equals(me.id()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cet achat ne vous appartient pas");
        return a;
    }

    private void fillProduitAndDates(Achat a) {
        if (a.getProduit() == null || a.getProduit().getId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le produit est obligatoire");
        Produit p = produitService.findById(a.getProduit().getId());
        a.setProduit(p);

        if (a.getDateAchat().isAfter(LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date d'achat ne peut pas être dans le futur");

        // RG1: end date = purchase date + duration (default = product's standard duration)
        if (a.getDureeGarantie() == null || a.getDureeGarantie() <= 0)
            a.setDureeGarantie(p.getDureeGarantieStandard());
        a.setDateFinGarantie(a.getDateAchat().plusMonths(a.getDureeGarantie()));
    }

    /** ownerId : propriétaire de l'achat (l'utilisateur connecté, ou un client choisi par l'admin). */
    public Achat save(Achat a, Long ownerId, MultipartFile facture, MultipartFile photo) {
        User u = userRepo.findById(ownerId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : " + ownerId));
        a.setId(null);
        a.setRappelEnvoye(false);
        a.setUser(u);
        fillProduitAndDates(a);

        a.setFacturePdf(storage.save(facture, "factures", FileStorageService.PDF, MAX_PDF));
        if (photo != null && !photo.isEmpty())
            a.setPhotoAchat(storage.save(photo, "achats", FileStorageService.IMAGES, MAX_IMG));
        return repo.save(a);
    }

    public Achat update(Long id, CurrentUser me, Achat in, MultipartFile facture, MultipartFile photo) {
        Achat existing = findAccessible(id, me);
        existing.setDateAchat(in.getDateAchat());
        existing.setPrixPaye(in.getPrixPaye());
        existing.setMagasin(in.getMagasin());
        existing.setNumeroSerie(in.getNumeroSerie());
        existing.setDureeGarantie(in.getDureeGarantie());
        existing.setProduit(in.getProduit());
        LocalDate ancienneFin = existing.getDateFinGarantie();
        fillProduitAndDates(existing);
        if (!Objects.equals(ancienneFin, existing.getDateFinGarantie()))
            existing.setRappelEnvoye(false);          // nouvelle échéance : le rappel redevient possible

        if (facture != null && !facture.isEmpty()) {
            String name = storage.save(facture, "factures", FileStorageService.PDF, MAX_PDF);
            storage.delete("factures", existing.getFacturePdf());   // RG6
            existing.setFacturePdf(name);
        }
        if (photo != null && !photo.isEmpty()) {
            String name = storage.save(photo, "achats", FileStorageService.IMAGES, MAX_IMG);
            storage.delete("achats", existing.getPhotoAchat());     // RG6
            existing.setPhotoAchat(name);
        }
        return repo.save(existing);
    }

    public void delete(Long id, CurrentUser me) {
        Achat existing = findAccessible(id, me);
        repo.delete(existing);                                      // 409 s'il a des réclamations
        storage.delete("factures", existing.getFacturePdf());
        storage.delete("achats", existing.getPhotoAchat());
    }
}

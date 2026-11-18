package com.sip.wbx.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.sip.wbx.entities.Achat;
import com.sip.wbx.entities.Reclamation;
import com.sip.wbx.repositories.ReclamationRepository;
import com.sip.wbx.security.CurrentUser;
import com.sip.wbx.entities.User;

@Service
public class ReclamationService {

    private static final long MAX_PDF = 5 * 1024 * 1024;   // 5 MB

    // RG4: allowed transitions
    private static final Map<String, Set<String>> NEXT = Map.of(
        "OUVERTE", Set.of("EN_COURS"),
        "EN_COURS", Set.of("RESOLUE", "REFUSEE"),
        "RESOLUE", Set.of(),
        "REFUSEE", Set.of());

    private final ReclamationRepository repo;
    private final AchatService achatService;
    private final FileStorageService storage;
    private final MailService mail;
    private final MessageService messages;

    public ReclamationService(ReclamationRepository repo, AchatService achatService,
                              FileStorageService storage, MailService mail, MessageService messages) {
        this.repo = repo;
        this.achatService = achatService;
        this.storage = storage;
        this.mail = mail;
        this.messages = messages;
    }

    // RG5 : un client voit ses réclamations, l'admin voit tout
    public List<Reclamation> findVisible(CurrentUser me) {
        return me.admin() ? repo.findAll() : repo.findByAchatUserId(me.id());
    }

    public List<Reclamation> findAll() {
        return repo.findAll();
    }

    private Reclamation findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Réclamation introuvable : " + id));
    }

    public Reclamation findAccessible(Long id, CurrentUser me) {
        Reclamation r = findById(id);
        if (!me.admin() && !r.getAchat().getUser().getId().equals(me.id()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette réclamation ne vous appartient pas");
        return r;
    }

    public Reclamation save(Reclamation r, Long achatId, CurrentUser me, MultipartFile justificatif) {
        Achat achat = achatService.findAccessible(achatId, me);

        // RG3 : pas de réclamation sur une garantie expirée
        if ("EXPIREE".equals(achat.getStatutGarantie()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La garantie de cet achat est expirée : réclamation impossible");

        r.setId(null);
        r.setAchat(achat);
        r.setStatut("OUVERTE");
        r.setDateOuverture(LocalDate.now());
        if (justificatif != null && !justificatif.isEmpty())
            r.setJustificatifPdf(storage.save(justificatif, "reclamations", FileStorageService.PDF, MAX_PDF));
        return repo.save(r);
    }

    // RG4 : cycle des statuts (admin)
    public Reclamation changeStatut(Long id, String statut, String commentaire) {
        Reclamation r = findById(id);
        if (!NEXT.getOrDefault(r.getStatut(), Set.of()).contains(statut))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Transition interdite : " + r.getStatut() + " -> " + statut);
        if (commentaire != null && commentaire.trim().length() > 1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le message est limité à 1000 caractères");
        r.setStatut(statut);
        if (commentaire != null && !commentaire.isBlank()) r.setReponseAdmin(commentaire.trim());
        Reclamation saved = repo.save(r);

        // le client est prévenu par e-mail (envoi en arrière-plan)
        User client = saved.getAchat().getUser();
        String produit = saved.getAchat().getProduit().getMarque() + " " + saved.getAchat().getProduit().getNom();
        mail.notifyReclamationStatut(client.getEmail(), client.getPrenom(), saved.getId(),
            produit, saved.getStatut(), saved.getReponseAdmin());

        // le message de l'admin est aussi ajouté à la conversation du client (messagerie)
        if (commentaire != null && !commentaire.isBlank())
            messages.postStatusNote(client.getId(), saved.getId(),
                "Réclamation n°" + saved.getId() + " (" + produit + ") : " + commentaire.trim());
        return saved;
    }

    public void delete(Long id) {
        Reclamation r = findById(id);
        repo.delete(r);
        storage.delete("reclamations", r.getJustificatifPdf());   // RG6
    }
}

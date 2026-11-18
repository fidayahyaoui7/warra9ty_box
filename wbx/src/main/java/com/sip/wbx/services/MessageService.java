package com.sip.wbx.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.sip.wbx.dto.ConversationDto;
import com.sip.wbx.dto.MessageDto;
import com.sip.wbx.dto.MessageRequest;
import com.sip.wbx.entities.Message;
import com.sip.wbx.entities.Reclamation;
import com.sip.wbx.entities.User;
import com.sip.wbx.repositories.MessageRepository;
import com.sip.wbx.repositories.ReclamationRepository;
import com.sip.wbx.repositories.UserRepository;

/**
 * Messagerie client / équipe. Chaque client a une seule conversation :
 *  - le client écrit à l'équipe et lit les réponses,
 *  - l'admin voit la boîte de réception (une ligne par client) et répond dans la conversation.
 * L'autre partie est prévenue par e-mail (en arrière-plan).
 */
@Service
public class MessageService {

    private final MessageRepository repo;
    private final UserRepository users;
    private final ReclamationRepository claims;
    private final MailService mail;

    public MessageService(MessageRepository repo, UserRepository users,
                          ReclamationRepository claims, MailService mail) {
        this.repo = repo;
        this.users = users;
        this.claims = claims;
        this.mail = mail;
    }

    private User client(Long id) {
        User u = users.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable : " + id));
        if (u.getRoles().stream().noneMatch(r -> "CLIENT".equals(r.getLibelle())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La messagerie est réservée aux comptes clients");
        return u;
    }

    // la réclamation liée (facultative) doit appartenir au client de la conversation
    private void checkReclamation(Long reclamationId, Long clientId) {
        if (reclamationId == null) return;
        Reclamation r = claims.findById(reclamationId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Réclamation introuvable : " + reclamationId));
        if (!r.getAchat().getUser().getId().equals(clientId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette réclamation ne concerne pas ce client");
    }

    private Message save(User client, boolean deAdmin, String contenu, Long reclamationId) {
        Message m = new Message();
        m.setClient(client);
        m.setDeAdmin(deAdmin);
        m.setContenu(contenu.trim());
        m.setReclamationId(reclamationId);
        return repo.save(m);
    }

    // ---------- Lecture ----------

    @Transactional(readOnly = true)
    public List<MessageDto> thread(Long clientId) {
        client(clientId);
        return repo.findByClientIdOrderByDateEnvoiAscIdAsc(clientId).stream().map(MessageDto::of).toList();
    }

    // Boîte de réception de l'admin : une ligne par client, la plus récente en premier
    @Transactional(readOnly = true)
    public List<ConversationDto> conversations() {
        Map<Long, Message> last = new LinkedHashMap<>();
        Map<Long, Long> unread = new LinkedHashMap<>();
        for (Message m : repo.findAllByOrderByDateEnvoiDescIdDesc()) {
            Long cid = m.getClient().getId();
            last.putIfAbsent(cid, m);
            if (!m.isDeAdmin() && !m.isLu()) unread.merge(cid, 1L, Long::sum);
        }
        return last.values().stream().map(m -> {
            User c = m.getClient();
            return new ConversationDto(c.getId(), c.getNom(), c.getPrenom(), c.getEmail(), c.getPhotoProfil(),
                m.getContenu(), m.isDeAdmin(), m.getDateEnvoi(), unread.getOrDefault(c.getId(), 0L));
        }).toList();
    }

    public long unreadForClient(Long clientId) {
        return repo.countByClientIdAndDeAdminTrueAndLuFalse(clientId);
    }

    public long unreadForAdmin() {
        return repo.countByDeAdminFalseAndLuFalse();
    }

    // ---------- Écriture ----------

    @Transactional
    public MessageDto sendAsClient(Long clientId, MessageRequest in) {
        User c = client(clientId);
        checkReclamation(in.reclamationId(), clientId);
        Message saved = save(c, false, in.contenu(), in.reclamationId());
        mail.notifyAdminNewMessage(c, saved.getContenu());
        return MessageDto.of(saved);
    }

    @Transactional
    public MessageDto sendAsAdmin(Long clientId, MessageRequest in) {
        User c = client(clientId);
        checkReclamation(in.reclamationId(), clientId);
        Message saved = save(c, true, in.contenu(), in.reclamationId());
        mail.notifyClientNewMessage(c, saved.getContenu());
        return MessageDto.of(saved);
    }

    /**
     * Message posté automatiquement par l'admin quand il change le statut d'une réclamation avec un commentaire :
     * il apparaît dans la conversation. Pas d'e-mail ici, le client reçoit déjà l'e-mail de changement de statut.
     */
    @Transactional
    public void postStatusNote(Long clientId, Long reclamationId, String contenu) {
        save(client(clientId), true, contenu, reclamationId);
    }

    // ---------- Lu / non lu ----------

    @Transactional
    public int markReadByClient(Long clientId) {          // le client a lu les messages de l'admin
        return repo.markRead(clientId, true);
    }

    @Transactional
    public int markReadByAdmin(Long clientId) {           // l'admin a lu les messages du client
        return repo.markRead(clientId, false);
    }
}

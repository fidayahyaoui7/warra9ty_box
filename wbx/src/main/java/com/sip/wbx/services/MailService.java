package com.sip.wbx.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.sip.wbx.entities.User;
import jakarta.annotation.PreDestroy;

/**
 * Envoi d'emails. Si SMTP n'est pas configuré (spring.mail.host absent) ou en erreur,
 * l'email est écrit dans les logs et l'application continue de fonctionner.
 * Les emails déclenchés par une requête HTTP partent en arrière-plan (sendLater) :
 * la réponse de l'API n'attend jamais le serveur SMTP.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final String adminEmail;
    private final String frontendUrl;
    private final String from;
    private final int reminderDays;
    private final ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "wbx-mail");
        t.setDaemon(true);
        return t;
    });

    public MailService(ObjectProvider<JavaMailSender> senderProvider,
                       @Value("${app.admin.email}") String adminEmail,
                       @Value("${app.frontend.url}") String frontendUrl,
                       @Value("${spring.mail.username:no-reply@wbx.tn}") String from,
                       @Value("${app.reminder.days:14}") int reminderDays) {
        this.senderProvider = senderProvider;
        this.adminEmail = adminEmail;
        this.frontendUrl = frontendUrl;
        this.from = from;
        this.reminderDays = reminderDays;
    }

    /** true si un serveur SMTP est configuré. */
    public boolean isConfigured() {
        return senderProvider.getIfAvailable() != null;
    }

    /** Envoi immédiat. Renvoie true seulement si le serveur SMTP a accepté l'email. */
    public boolean send(String to, String subject, String text) {
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP non configuré - email non envoyé.\nÀ : {}\nObjet : {}\n{}", to, subject, text);
            return false;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(text);
            sender.send(m);
            log.info("Email envoyé à {} : {}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("Échec d'envoi de l'email à {} : {}", to, e.getMessage());
            return false;
        }
    }

    /** Envoi en arrière-plan (utilisé par les endpoints HTTP). */
    public void sendLater(String to, String subject, String text) {
        pool.submit(() -> send(to, subject, text));
    }

    @PreDestroy
    void shutdown() {
        pool.shutdown();
    }

    // 1) Inscription : bienvenue à l'utilisateur
    public void sendWelcome(User u) {
        sendLater(u.getEmail(),
            "[Warra9tyBox] Bienvenue sur Warra9tyBox",
            "Bonjour " + u.getPrenom() + ",\n\n"
            + "Bienvenue sur Warra9tyBox ! Votre compte a bien été créé.\n"
            + "Un administrateur doit l'activer avant votre première connexion : "
            + "vous recevrez un nouvel e-mail dès que ce sera fait.\n\n"
            + "Une fois connecté, vous pourrez :\n"
            + "  - enregistrer vos achats avec leur facture,\n"
            + "  - suivre la fin de vos garanties (rappel par e-mail " + reminderDays + " jours avant l'échéance),\n"
            + "  - ouvrir une réclamation en cas de problème.\n\n"
            + "À bientôt,\nL'équipe Warra9tyBox");
    }

    // Inscription : prévenir l'admin qu'un compte attend son activation
    public void notifyAdminNewUser(User u) {
        sendLater(adminEmail,
            "[Warra9tyBox] Nouveau compte à activer : " + u.getEmail(),
            "Bonjour,\n\n" + u.getPrenom() + " " + u.getNom() + " (" + u.getEmail() + ") vient de créer un compte.\n"
            + "Le compte reste inactif tant que vous ne l'avez pas activé.\n\n"
            + "Activer le compte : " + frontendUrl + "/#/utilisateurs\n\n"
            + "Warra9tyBox");
    }

    // Activation : prévenir l'utilisateur
    public void notifyUserActivated(User u) {
        sendLater(u.getEmail(),
            "[Warra9tyBox] Votre compte est activé",
            "Bonjour " + u.getPrenom() + ",\n\nVotre compte Warra9tyBox est activé. Vous pouvez vous connecter :\n"
            + frontendUrl + "/#/connexion\n\nWarra9tyBox");
    }

    // 3) Le statut d'une réclamation change : prévenir son auteur
    public void notifyReclamationStatut(String to, String prenom, Long reclamationId,
                                        String produit, String statut, String commentaire) {
        String objet;
        String corps;
        switch (statut) {
            case "EN_COURS" -> {
                objet = "Votre réclamation n°" + reclamationId + " est en cours de traitement";
                corps = "Votre réclamation n°" + reclamationId + " concernant « " + produit
                    + " » est prise en charge par notre équipe.\nVous serez prévenu dès qu'elle sera traitée.";
            }
            case "RESOLUE" -> {
                objet = "Votre réclamation n°" + reclamationId + " est résolue";
                corps = "Bonne nouvelle : le problème signalé sur « " + produit
                    + " » (réclamation n°" + reclamationId + ") a été traité. Votre réclamation est résolue.";
            }
            case "REFUSEE" -> {
                objet = "Votre réclamation n°" + reclamationId + " a été refusée";
                corps = "Après examen, votre réclamation n°" + reclamationId + " concernant « " + produit
                    + " » n'a pas pu être acceptée.";
            }
            default -> {
                objet = "Votre réclamation n°" + reclamationId + " a été mise à jour";
                corps = "Le statut de votre réclamation n°" + reclamationId + " (« " + produit + " ») est maintenant : " + statut + ".";
            }
        }
        String note = (commentaire == null || commentaire.isBlank()) ? ""
            : "\n\nMessage de l'administrateur :\n" + commentaire.trim();
        sendLater(to, "[Warra9tyBox] " + objet,
            "Bonjour " + prenom + ",\n\n" + corps + note
            + "\n\nSuivre mes réclamations : " + frontendUrl + "/#/reclamations\n\nWarra9tyBox");
    }

    // Messagerie : le client a écrit à l'équipe -> e-mail à l'admin
    public void notifyAdminNewMessage(User client, String contenu) {
        sendLater(adminEmail,
            "[Warra9tyBox] Nouveau message de " + client.getPrenom() + " " + client.getNom(),
            "Bonjour,\n\n" + client.getPrenom() + " " + client.getNom() + " (" + client.getEmail() + ") vous a écrit :\n\n"
            + extrait(contenu) + "\n\nRépondre : " + frontendUrl + "/#/messages?client=" + client.getId() + "\n\nWarra9tyBox");
    }

    // Messagerie : l'admin a répondu -> e-mail au client
    public void notifyClientNewMessage(User client, String contenu) {
        sendLater(client.getEmail(),
            "[Warra9tyBox] Nouveau message de l'équipe Warra9tyBox",
            "Bonjour " + client.getPrenom() + ",\n\nVous avez reçu un message de l'équipe Warra9tyBox :\n\n"
            + extrait(contenu) + "\n\nRépondre : " + frontendUrl + "/#/messages\n\nWarra9tyBox");
    }

    private static String extrait(String s) {
        String t = s == null ? "" : s.trim();
        return "« " + (t.length() > 500 ? t.substring(0, 500) + "…" : t) + " »";
    }
}

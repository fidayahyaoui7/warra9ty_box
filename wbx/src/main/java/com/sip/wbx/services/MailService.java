package com.sip.wbx.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.sip.wbx.entities.User;

/**
 * Envoi d'emails. Si SMTP n'est pas configuré (spring.mail.host absent) ou en erreur,
 * l'email est écrit dans les logs et l'application continue de fonctionner.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final String adminEmail;
    private final String frontendUrl;
    private final String from;

    public MailService(ObjectProvider<JavaMailSender> senderProvider,
                       @Value("${app.admin.email}") String adminEmail,
                       @Value("${app.frontend.url}") String frontendUrl,
                       @Value("${spring.mail.username:no-reply@wbx.tn}") String from) {
        this.senderProvider = senderProvider;
        this.adminEmail = adminEmail;
        this.frontendUrl = frontendUrl;
        this.from = from;
    }

    public void send(String to, String subject, String text) {
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP non configuré - email non envoyé.\nÀ : {}\nObjet : {}\n{}", to, subject, text);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(text);
            sender.send(m);
            log.info("Email envoyé à {} : {}", to, subject);
        } catch (Exception e) {
            log.error("Échec d'envoi de l'email à {} : {}", to, e.getMessage());
        }
    }

    // Inscription : prévenir l'admin qu'un compte attend son activation
    public void notifyAdminNewUser(User u) {
        send(adminEmail,
            "[Warra9tyBox] Nouveau compte à activer : " + u.getEmail(),
            "Bonjour,\n\n" + u.getPrenom() + " " + u.getNom() + " (" + u.getEmail() + ") vient de créer un compte.\n"
            + "Le compte reste inactif tant que vous ne l'avez pas activé.\n\n"
            + "Activer le compte : " + frontendUrl + "/#/utilisateurs\n\n"
            + "Warra9tyBox");
    }

    // Activation : prévenir l'utilisateur
    public void notifyUserActivated(User u) {
        send(u.getEmail(),
            "[Warra9tyBox] Votre compte est activé",
            "Bonjour " + u.getPrenom() + ",\n\nVotre compte Warra9tyBox est activé. Vous pouvez vous connecter :\n"
            + frontendUrl + "/#/connexion\n\nWarra9tyBox");
    }
}

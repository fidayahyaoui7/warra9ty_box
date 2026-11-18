package com.sip.wbx.controllers;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.sip.wbx.services.MailService;
import com.sip.wbx.services.WarrantyReminderScheduler;

/** Outils d'administration (ADMIN seulement, voir SecurityConfig) : test des e-mails et des rappels. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final WarrantyReminderScheduler reminders;
    private final MailService mail;

    public AdminController(WarrantyReminderScheduler reminders, MailService mail) {
        this.reminders = reminders;
        this.mail = mail;
    }

    // Lance tout de suite le rappel de fin de garantie (sans attendre 8 h). force=true : renvoie même si déjà envoyé.
    @PostMapping("/rappels")
    public Map<String, Integer> runReminders(@RequestParam(defaultValue = "false") boolean force) {
        return reminders.runReminders(force);
    }

    // Envoie un e-mail de test (par défaut à l'adresse de l'admin connecté) pour vérifier la configuration SMTP.
    @PostMapping("/mail/test")
    public Map<String, Object> testMail(@RequestParam(required = false) String to,
                                        @AuthenticationPrincipal Jwt jwt) {
        String dest = (to == null || to.isBlank()) ? jwt.getSubject() : to;
        boolean configure = mail.isConfigured();
        boolean envoye = mail.send(dest, "[Warra9tyBox] Test d'envoi d'e-mail",
            "Si vous lisez ce message, la configuration SMTP de Warra9tyBox fonctionne.");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("destinataire", dest);
        res.put("smtpConfigure", configure);
        res.put("envoye", envoye);
        return res;
    }
}

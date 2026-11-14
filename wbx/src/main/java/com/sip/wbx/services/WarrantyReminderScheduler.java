package com.sip.wbx.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.sip.wbx.entities.Achat;
import com.sip.wbx.entities.User;
import com.sip.wbx.repositories.AchatRepository;

/** Chaque jour : un email par client dont au moins une garantie expire dans 30 jours ou moins. */
@Component
public class WarrantyReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarrantyReminderScheduler.class);
    private static final DateTimeFormatter FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AchatRepository achatRepo;
    private final MailService mail;

    public WarrantyReminderScheduler(AchatRepository achatRepo, MailService mail) {
        this.achatRepo = achatRepo;
        this.mail = mail;
    }

    @Scheduled(cron = "${app.reminder.cron:0 0 8 * * *}")
    public void sendDailyReminders() {
        runReminders();
    }

    /** Renvoie le nombre d'emails envoyés (utile pour les tests). */
    public int runReminders() {
        LocalDate today = LocalDate.now();
        List<Achat> expiring = achatRepo.findByDateFinGarantieBetween(today, today.plusDays(30));

        Map<Long, StringBuilder> perUser = new LinkedHashMap<>();
        Map<Long, User> users = new LinkedHashMap<>();
        for (Achat a : expiring) {
            User u = a.getUser();
            if (!u.isEnabled()) continue;
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, a.getDateFinGarantie());
            users.put(u.getId(), u);
            perUser.computeIfAbsent(u.getId(), k -> new StringBuilder())
                .append("  - ").append(a.getProduit().getMarque()).append(' ').append(a.getProduit().getNom())
                .append(" : fin de garantie le ").append(a.getDateFinGarantie().format(FR))
                .append(" (dans ").append(days).append(days > 1 ? " jours)" : " jour)").append('\n');
        }

        perUser.forEach((uid, lines) -> mail.send(users.get(uid).getEmail(),
            "[Warra9tyBox] Une garantie arrive bientôt à échéance",
            "Bonjour " + users.get(uid).getPrenom() + ",\n\nCes garanties se terminent dans moins de 30 jours :\n\n" + lines
            + "\nSi un produit pose problème, ouvrez une réclamation avant la date indiquée.\n\nWarra9tyBox"));

        log.info("Rappels de garantie : {} email(s) pour {} achat(s)", perUser.size(), expiring.size());
        return perUser.size();
    }
}

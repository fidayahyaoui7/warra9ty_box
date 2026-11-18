package com.sip.wbx.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.sip.wbx.entities.Achat;
import com.sip.wbx.entities.User;
import com.sip.wbx.repositories.AchatRepository;

/**
 * Chaque jour à 8 h : un e-mail par client dont une garantie se termine dans moins de
 * "app.reminder.days" jours (14 par défaut). Un achat n'est rappelé qu'une seule fois
 * (drapeau rappelEnvoye), même si l'application a été arrêtée quelques jours.
 */
@Component
public class WarrantyReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarrantyReminderScheduler.class);
    private static final DateTimeFormatter FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AchatRepository achatRepo;
    private final MailService mail;
    private final int days;

    public WarrantyReminderScheduler(AchatRepository achatRepo, MailService mail,
                                     @Value("${app.reminder.days:14}") int days) {
        this.achatRepo = achatRepo;
        this.mail = mail;
        this.days = days;
    }

    @Scheduled(cron = "${app.reminder.cron:0 0 8 * * *}")
    public void sendDailyReminders() {
        runReminders(false);
    }

    /**
     * @param force true : ignore le drapeau "rappel déjà envoyé" (démonstration / test)
     * @return clientsConcernes, achatsConcernes, emailsEnvoyes
     */
    public Map<String, Integer> runReminders(boolean force) {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(days);
        List<Achat> expiring = force
            ? achatRepo.findByDateFinGarantieBetween(today, limit)
            : achatRepo.findRappelsAEnvoyer(today, limit);

        Map<Long, List<Achat>> parClient = new LinkedHashMap<>();
        for (Achat a : expiring) {
            User u = a.getUser();
            if (!u.isEnabled()) continue;
            parClient.computeIfAbsent(u.getId(), k -> new ArrayList<>()).add(a);
        }

        int envoyes = 0;
        List<Achat> aMarquer = new ArrayList<>();
        for (List<Achat> achats : parClient.values()) {
            User u = achats.get(0).getUser();
            StringBuilder lignes = new StringBuilder();
            for (Achat a : achats) {
                long reste = ChronoUnit.DAYS.between(today, a.getDateFinGarantie());
                lignes.append("  - ").append(a.getProduit().getMarque()).append(' ').append(a.getProduit().getNom())
                    .append(" : fin de garantie le ").append(a.getDateFinGarantie().format(FR))
                    .append(reste == 0 ? " (aujourd'hui)" : " (dans " + reste + (reste > 1 ? " jours)" : " jour)"))
                    .append('\n');
            }
            boolean ok = mail.send(u.getEmail(),
                "[Warra9tyBox] Votre garantie se termine bientôt",
                "Bonjour " + u.getPrenom() + ",\n\nCes garanties se terminent dans moins de " + days + " jours :\n\n" + lignes
                + "\nSi un produit pose problème, ouvrez une réclamation avant la date indiquée.\n\nWarra9tyBox");
            if (ok) {
                envoyes++;
                aMarquer.addAll(achats);
            }
        }
        aMarquer.forEach(a -> a.setRappelEnvoye(true));
        achatRepo.saveAll(aMarquer);

        log.info("Rappels de garantie ({} jours) : {} client(s), {} achat(s), {} e-mail(s) envoyé(s)",
            days, parClient.size(), expiring.size(), envoyes);
        Map<String, Integer> res = new LinkedHashMap<>();
        res.put("clientsConcernes", parClient.size());
        res.put("achatsConcernes", expiring.size());
        res.put("emailsEnvoyes", envoyes);
        return res;
    }
}

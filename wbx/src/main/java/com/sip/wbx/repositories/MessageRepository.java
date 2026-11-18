package com.sip.wbx.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.sip.wbx.entities.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByClientIdOrderByDateEnvoiAscIdAsc(Long clientId);

    List<Message> findAllByOrderByDateEnvoiDescIdDesc();

    // non lus pour le client : messages de l'admin pas encore lus
    long countByClientIdAndDeAdminTrueAndLuFalse(Long clientId);

    // non lus pour l'admin : messages des clients pas encore lus
    long countByDeAdminFalseAndLuFalse();

    // marque comme lus les messages reçus par un destinataire (fromAdmin = true : le client lit l'admin)
    @Modifying(clearAutomatically = true)
    @Query("update Message m set m.lu = true where m.client.id = :clientId and m.deAdmin = :fromAdmin and m.lu = false")
    int markRead(@Param("clientId") Long clientId, @Param("fromAdmin") boolean fromAdmin);
}

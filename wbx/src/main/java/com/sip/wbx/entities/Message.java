package com.sip.wbx.entities;

import java.time.LocalDateTime;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.*;
import lombok.*;

/**
 * Message de la boîte de messagerie. Chaque client a UNE conversation avec l'équipe (les administrateurs) :
 * "client" est le client de la conversation, "deAdmin" dit qui a écrit le message.
 */
@Entity
@Getter @Setter @NoArgsConstructor
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    @OnDelete(action = OnDeleteAction.CASCADE)   // supprimer un client supprime sa conversation
    private User client;

    private boolean deAdmin;                     // true : écrit par l'admin, false : écrit par le client

    @Column(nullable = false, length = 2000)
    private String contenu;

    private LocalDateTime dateEnvoi = LocalDateTime.now();

    private boolean lu = false;                  // lu par son destinataire

    private Long reclamationId;                  // réclamation concernée (facultatif, sans clé étrangère)
}

package com.sip.wbx.dto;

import java.time.LocalDateTime;

/** Une ligne de la boîte de réception de l'admin : un client, son dernier message et le nombre de messages non lus. */
public record ConversationDto(Long clientId, String nom, String prenom, String email, String photoProfil,
                              String dernierMessage, boolean dernierDeAdmin, LocalDateTime dateDernierMessage,
                              long nonLus) {}

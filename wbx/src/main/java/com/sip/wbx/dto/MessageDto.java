package com.sip.wbx.dto;

import java.time.LocalDateTime;
import com.sip.wbx.entities.Message;

public record MessageDto(Long id, Long clientId, boolean deAdmin, String contenu,
                         LocalDateTime dateEnvoi, boolean lu, Long reclamationId) {

    public static MessageDto of(Message m) {
        return new MessageDto(m.getId(), m.getClient().getId(), m.isDeAdmin(), m.getContenu(),
            m.getDateEnvoi(), m.isLu(), m.getReclamationId());
    }
}

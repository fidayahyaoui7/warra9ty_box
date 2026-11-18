package com.sip.wbx.controllers;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.sip.wbx.dto.ConversationDto;
import com.sip.wbx.dto.MessageDto;
import com.sip.wbx.dto.MessageRequest;
import com.sip.wbx.services.MessageService;
import jakarta.validation.Valid;

/** Messagerie côté ADMIN : boîte de réception et réponses (réservé à l'ADMIN : /api/admin/**). */
@RestController
@RequestMapping("/api/admin/messages")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminMessageController {

    private final MessageService service;

    public AdminMessageController(MessageService service) {
        this.service = service;
    }

    @GetMapping("/conversations")
    public List<ConversationDto> conversations() {
        return service.conversations();
    }

    @GetMapping("/non-lus")
    public Map<String, Long> unread() {
        return Map.of("count", service.unreadForAdmin());
    }

    @GetMapping("/{clientId}")
    public List<MessageDto> thread(@PathVariable Long clientId) {
        return service.thread(clientId);
    }

    @PostMapping("/{clientId}")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDto send(@PathVariable Long clientId, @Valid @RequestBody MessageRequest req) {
        return service.sendAsAdmin(clientId, req);
    }

    @PatchMapping("/{clientId}/lu")
    public Map<String, Integer> markRead(@PathVariable Long clientId) {
        return Map.of("updated", service.markReadByAdmin(clientId));
    }
}

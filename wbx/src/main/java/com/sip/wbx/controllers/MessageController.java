package com.sip.wbx.controllers;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.sip.wbx.dto.MessageDto;
import com.sip.wbx.dto.MessageRequest;
import com.sip.wbx.security.CurrentUser;
import com.sip.wbx.services.MessageService;
import jakarta.validation.Valid;

/** Messagerie côté CLIENT : sa conversation avec l'équipe (réservé au rôle CLIENT, voir SecurityConfig). */
@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "http://localhost:4200")
public class MessageController {

    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    @GetMapping
    public List<MessageDto> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.thread(CurrentUser.of(jwt).id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDto send(@Valid @RequestBody MessageRequest req, @AuthenticationPrincipal Jwt jwt) {
        return service.sendAsClient(CurrentUser.of(jwt).id(), req);
    }

    @GetMapping("/non-lus")
    public Map<String, Long> unread(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", service.unreadForClient(CurrentUser.of(jwt).id()));
    }

    @PatchMapping("/lu")
    public Map<String, Integer> markRead(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("updated", service.markReadByClient(CurrentUser.of(jwt).id()));
    }
}

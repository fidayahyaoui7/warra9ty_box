package com.sip.wbx.controllers;

import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.sip.wbx.entities.Reclamation;
import com.sip.wbx.security.CurrentUser;
import com.sip.wbx.services.FileStorageService;
import com.sip.wbx.services.ReclamationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reclamations")
@CrossOrigin(origins = "http://localhost:4200")
public class ReclamationController {

    private final ReclamationService service;
    private final FileStorageService storage;

    public ReclamationController(ReclamationService service, FileStorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    // Client : ses réclamations. Admin : toutes les réclamations.
    @GetMapping
    public List<Reclamation> list(@AuthenticationPrincipal Jwt jwt) {
        return service.findVisible(CurrentUser.of(jwt));
    }

    @GetMapping("/all")                                  // admin seulement (SecurityConfig)
    public List<Reclamation> listAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Reclamation get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.findAccessible(id, CurrentUser.of(jwt));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Reclamation add(@RequestParam Long achatId,
                           @Valid @RequestPart("reclamation") Reclamation r,
                           @RequestPart(value = "justificatif", required = false) MultipartFile justificatif,
                           @AuthenticationPrincipal Jwt jwt) {
        return service.save(r, achatId, CurrentUser.of(jwt), justificatif);
    }

    @PatchMapping("/{id}/statut")                        // admin seulement (SecurityConfig)
    public Reclamation statut(@PathVariable Long id, @RequestParam String statut) {
        return service.changeStatut(id, statut);
    }

    @DeleteMapping("/{id}")                              // admin seulement (SecurityConfig)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/justificatif")
    public ResponseEntity<Resource> justificatif(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Reclamation r = service.findAccessible(id, CurrentUser.of(jwt));
        if (r.getJustificatifPdf() == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pas de justificatif");
        Resource res = storage.load("reclamations", r.getJustificatifPdf());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"justificatif-" + id + ".pdf\"")
            .body(res);
    }
}

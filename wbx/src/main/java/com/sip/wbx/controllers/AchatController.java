package com.sip.wbx.controllers;

import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.sip.wbx.entities.Achat;
import com.sip.wbx.security.CurrentUser;
import com.sip.wbx.services.AchatService;
import com.sip.wbx.services.FileStorageService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/achats")
@CrossOrigin(origins = "http://localhost:4200")
public class AchatController {

    private final AchatService service;
    private final FileStorageService storage;

    public AchatController(AchatService service, FileStorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    // Client : ses achats. Admin : tous les achats.
    @GetMapping
    public List<Achat> list(@AuthenticationPrincipal Jwt jwt) {
        return service.findVisible(CurrentUser.of(jwt));
    }

    @GetMapping("/{id}")
    public Achat get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.findAccessible(id, CurrentUser.of(jwt));
    }

    // L'achat appartient à l'utilisateur connecté (l'admin peut en plus choisir un client avec userId)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Achat add(@Valid @RequestPart("achat") Achat a,
                     @RequestPart("facture") MultipartFile facture,
                     @RequestPart(value = "photo", required = false) MultipartFile photo,
                     @RequestParam(required = false) Long userId,
                     @AuthenticationPrincipal Jwt jwt) {
        CurrentUser me = CurrentUser.of(jwt);
        Long owner = (me.admin() && userId != null) ? userId : me.id();
        return service.save(a, owner, facture, photo);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Achat update(@PathVariable Long id,
                        @Valid @RequestPart("achat") Achat a,
                        @RequestPart(value = "facture", required = false) MultipartFile facture,
                        @RequestPart(value = "photo", required = false) MultipartFile photo,
                        @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, CurrentUser.of(jwt), a, facture, photo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, CurrentUser.of(jwt));
    }

    // Facture PDF : propriétaire ou admin
    @GetMapping("/{id}/facture")
    public ResponseEntity<Resource> facture(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Achat a = service.findAccessible(id, CurrentUser.of(jwt));
        Resource r = storage.load("factures", a.getFacturePdf());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"facture-" + id + ".pdf\"")
            .body(r);
    }

    // Photo de l'achat : propriétaire ou admin
    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> photo(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Achat a = service.findAccessible(id, CurrentUser.of(jwt));
        if (a.getPhotoAchat() == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pas de photo pour cet achat");
        Resource r = storage.load("achats", a.getPhotoAchat());
        MediaType type = MediaTypeFactory.getMediaType(r).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).body(r);
    }
}

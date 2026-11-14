package com.sip.wbx.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.sip.wbx.dto.ProfileRequest;
import com.sip.wbx.dto.UserRequest;
import com.sip.wbx.entities.User;
import com.sip.wbx.security.CurrentUser;
import com.sip.wbx.services.UserService;
import jakarta.validation.Valid;

/**
 * /api/users/**   : réservé à l'ADMIN (voir SecurityConfig)
 * /api/profile    : chaque utilisateur connecté modifie son propre profil
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    // ---------- Admin ----------

    @GetMapping("/users")
    public List<User> list() {
        return service.findAll();
    }

    @GetMapping("/users/{id}")
    public User get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public User add(@Valid @RequestBody UserRequest req) {
        return service.create(req);
    }

    @PutMapping("/users/{id}")
    public User update(@PathVariable Long id, @Valid @RequestBody UserRequest req,
                       @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, req, CurrentUser.of(jwt).id());
    }

    // Activer / désactiver un compte : PATCH /api/users/5/enabled?value=true
    @PatchMapping("/users/{id}/enabled")
    public User enabled(@PathVariable Long id, @RequestParam boolean value,
                        @AuthenticationPrincipal Jwt jwt) {
        return service.setEnabled(id, value, CurrentUser.of(jwt).id());
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, CurrentUser.of(jwt).id());
    }

    // ---------- Mon profil ----------

    @GetMapping("/profile")
    public User profile(@AuthenticationPrincipal Jwt jwt) {
        return service.findById(CurrentUser.of(jwt).id());
    }

    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public User updateProfile(@AuthenticationPrincipal Jwt jwt,
                              @Valid @RequestPart("user") ProfileRequest req,
                              @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return service.updateProfile(CurrentUser.of(jwt).id(), req, photo);
    }
}

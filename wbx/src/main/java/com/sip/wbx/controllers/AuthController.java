package com.sip.wbx.controllers;

import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.sip.wbx.dto.LoginRequest;
import com.sip.wbx.dto.RegisterRequest;
import com.sip.wbx.entities.Role;
import com.sip.wbx.entities.User;
import com.sip.wbx.repositories.RoleRepository;
import com.sip.wbx.repositories.UserRepository;
import com.sip.wbx.services.JwtService;
import com.sip.wbx.services.MailService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MailService mail;

    public AuthController(UserRepository userRepo, RoleRepository roleRepo,
                          PasswordEncoder encoder, JwtService jwt, MailService mail) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mail = mail;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepo.findByEmail(req.email()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");

        Role client = roleRepo.findByLibelle("CLIENT").orElseThrow();
        User u = new User();
        u.setEmail(req.email());
        u.setPassword(encoder.encode(req.password()));
        u.setNom(req.nom());
        u.setPrenom(req.prenom());
        u.setTel(req.tel());
        u.setEnabled(false);                       // the admin must activate the account
        u.setRoles(Set.of(client));
        userRepo.save(u);
        mail.sendWelcome(u);                       // bienvenue à l'utilisateur
        mail.notifyAdminNewUser(u);                // l'admin reçoit un email pour activer le compte

        return Map.of("message", "Compte créé. Il sera utilisable après activation par l'administrateur.");
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        User u = userRepo.findByEmail(req.email()).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));

        if (!encoder.matches(req.password(), u.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        if (!u.isEnabled())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte en attente d'activation par l'administrateur");

        return Map.of(
            "token", jwt.generate(u),
            "id", u.getId(),
            "email", u.getEmail(),
            "nom", u.getNom() == null ? "" : u.getNom(),
            "prenom", u.getPrenom() == null ? "" : u.getPrenom(),
            "roles", u.getRoles().stream().map(Role::getLibelle).toList());
    }
}
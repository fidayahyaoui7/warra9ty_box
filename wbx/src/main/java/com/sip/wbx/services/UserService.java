package com.sip.wbx.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.sip.wbx.dto.ProfileRequest;
import com.sip.wbx.dto.UserRequest;
import com.sip.wbx.entities.Role;
import com.sip.wbx.entities.User;
import com.sip.wbx.repositories.RoleRepository;
import com.sip.wbx.repositories.UserRepository;

@Service
public class UserService {

    private static final long MAX_PHOTO = 1024 * 1024;   // 1 Mo

    private final UserRepository repo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final FileStorageService storage;
    private final MailService mail;

    public UserService(UserRepository repo, RoleRepository roleRepo, PasswordEncoder encoder,
                       FileStorageService storage, MailService mail) {
        this.repo = repo;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
        this.storage = storage;
        this.mail = mail;
    }

    public List<User> findAll() {
        return repo.findAll();
    }

    public User findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : " + id));
    }

    private Set<Role> resolveRoles(List<String> libelles) {
        Set<Role> roles = new HashSet<>();
        if (libelles == null || libelles.isEmpty()) libelles = List.of("CLIENT");
        for (String l : libelles) {
            roles.add(roleRepo.findByLibelle(l).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle inconnu : " + l)));
        }
        return roles;
    }

    public User create(UserRequest in) {
        if (repo.findByEmail(in.email()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        if (in.password() == null || in.password().length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe : 6 caractères minimum");
        User u = new User();
        u.setEmail(in.email());
        u.setPassword(encoder.encode(in.password()));
        u.setNom(in.nom());
        u.setPrenom(in.prenom());
        u.setTel(in.tel());
        u.setEnabled(in.enabled() == null || in.enabled());
        u.setRoles(resolveRoles(in.roles()));
        return repo.save(u);
    }

    public User update(Long id, UserRequest in, Long currentAdminId) {
        User u = findById(id);
        repo.findByEmail(in.email()).ifPresent(other -> {
            if (!other.getId().equals(id))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        });
        boolean wasEnabled = u.isEnabled();
        u.setEmail(in.email());
        u.setNom(in.nom());
        u.setPrenom(in.prenom());
        u.setTel(in.tel());
        if (in.password() != null && !in.password().isBlank()) {
            if (in.password().length() < 6)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe : 6 caractères minimum");
            u.setPassword(encoder.encode(in.password()));
        }
        if (in.enabled() != null) {
            if (id.equals(currentAdminId) && !in.enabled())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas désactiver votre propre compte");
            u.setEnabled(in.enabled());
        }
        if (in.roles() != null && !in.roles().isEmpty()) {
            Set<Role> roles = resolveRoles(in.roles());
            boolean stillAdmin = roles.stream().anyMatch(r -> "ADMIN".equals(r.getLibelle()));
            if (id.equals(currentAdminId) && !stillAdmin)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas retirer votre propre rôle Admin");
            u.setRoles(roles);
        }
        User saved = repo.save(u);
        if (!wasEnabled && saved.isEnabled()) mail.notifyUserActivated(saved);
        return saved;
    }

    // Activation / désactivation d'un compte (admin)
    public User setEnabled(Long id, boolean enabled, Long currentAdminId) {
        User u = findById(id);
        if (id.equals(currentAdminId) && !enabled)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas désactiver votre propre compte");
        boolean changed = u.isEnabled() != enabled;
        u.setEnabled(enabled);
        User saved = repo.save(u);
        if (changed && enabled) mail.notifyUserActivated(saved);
        return saved;
    }

    public void delete(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas supprimer votre propre compte");
        User u = findById(id);
        repo.delete(u);                                   // 409 si l'utilisateur a des achats
        storage.delete("profils", u.getPhotoProfil());
    }

    // Profil : chacun modifie le sien
    public User updateProfile(Long id, ProfileRequest in, MultipartFile photo) {
        User u = findById(id);
        u.setNom(in.nom());
        u.setPrenom(in.prenom());
        u.setTel(in.tel());
        if (in.password() != null && !in.password().isBlank()) {
            if (in.password().length() < 6)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe : 6 caractères minimum");
            u.setPassword(encoder.encode(in.password()));
        }
        if (photo != null && !photo.isEmpty()) {
            String name = storage.save(photo, "profils", FileStorageService.IMAGES, MAX_PHOTO);
            storage.delete("profils", u.getPhotoProfil());   // RG6
            u.setPhotoProfil(name);
        }
        return repo.save(u);
    }
}

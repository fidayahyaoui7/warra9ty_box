package com.sip.wbx.dto;

import java.util.List;
import jakarta.validation.constraints.*;

/** Création / modification d'un utilisateur par l'admin. Mot de passe facultatif en modification. */
public record UserRequest(
    @NotBlank @Email String email,
    String password,
    @NotBlank String nom,
    @NotBlank String prenom,
    String tel,
    Boolean enabled,
    List<String> roles) {}

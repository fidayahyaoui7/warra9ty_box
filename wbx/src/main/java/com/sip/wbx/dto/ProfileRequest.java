package com.sip.wbx.dto;

import jakarta.validation.constraints.*;

/** Modification de son propre profil (mot de passe facultatif). */
public record ProfileRequest(
    @NotBlank String nom,
    @NotBlank String prenom,
    String tel,
    String password) {}

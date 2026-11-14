package com.sip.wbx.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, message = "6 caractères minimum") String password,
    @NotBlank String nom,
    @NotBlank String prenom,
    String tel) {}
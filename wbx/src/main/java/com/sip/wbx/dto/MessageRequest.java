package com.sip.wbx.dto;

import jakarta.validation.constraints.*;

public record MessageRequest(
    @NotBlank(message = "Le message ne peut pas être vide")
    @Size(max = 2000, message = "2000 caractères maximum")
    String contenu,
    Long reclamationId) {}

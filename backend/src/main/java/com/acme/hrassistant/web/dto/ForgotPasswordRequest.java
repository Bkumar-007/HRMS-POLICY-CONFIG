package com.acme.hrassistant.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank String username,
        @NotBlank String channel
) {
}

package com.acme.hrassistant.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String username,
        @NotBlank String resetToken,
        @NotBlank String newPassword
) {
}

package com.acme.hrassistant.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank String username,
        @NotBlank String otp
) {
}

package com.acme.hrassistant.web.dto;

public record VerifyOtpResponse(
        boolean verified,
        String message,
        String resetToken
) {
}

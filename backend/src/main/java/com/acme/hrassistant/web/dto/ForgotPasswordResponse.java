package com.acme.hrassistant.web.dto;

public record ForgotPasswordResponse(
        boolean sent,
        String message,
        String channel,
        String maskedDestination
) {
}

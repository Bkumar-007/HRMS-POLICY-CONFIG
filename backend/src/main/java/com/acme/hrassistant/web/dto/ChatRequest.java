package com.acme.hrassistant.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank @Size(max = 4000) String message,
        String conversationId,
        /** AUTO | EN | HI — reply language; tools stay the same. */
        @Size(max = 8) String preferredLanguage
) {
    public ChatRequest(String message, String conversationId) {
        this(message, conversationId, "AUTO");
    }
}

package com.acme.hrassistant.web.dto;

import java.util.List;

public record ChatResponse(
        String conversationId,
        String message,
        List<CitationDto> citations,
        List<PendingActionDto> pendingActions
) {
    public record CitationDto(String source, String section, String snippet) {
    }

    public record PendingActionDto(String id, String type, String summary) {
    }
}
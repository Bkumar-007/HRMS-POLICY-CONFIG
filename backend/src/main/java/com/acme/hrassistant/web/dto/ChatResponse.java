package com.acme.hrassistant.web.dto;

import java.util.List;

public record ChatResponse(
        String conversationId,
        String message,
        List<CitationDto> citations,
        List<PendingActionDto> pendingActions,
        List<QuizQuestionDto> quiz
) {
    public ChatResponse(
            String conversationId,
            String message,
            List<CitationDto> citations,
            List<PendingActionDto> pendingActions) {
        this(conversationId, message, citations, pendingActions, List.of());
    }

    public record CitationDto(String policyId, String source, String section, String snippet) {
        public CitationDto(String source, String section, String snippet) {
            this(null, source, section, snippet);
        }
    }

    public record PendingActionDto(String id, String type, String summary) {
    }

    public record QuizQuestionDto(
            String id,
            String prompt,
            List<String> options,
            int correctIndex,
            String explanation,
            String policyName,
            String section
    ) {
    }
}

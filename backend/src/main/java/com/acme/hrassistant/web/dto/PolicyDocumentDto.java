package com.acme.hrassistant.web.dto;

import com.acme.hrassistant.domain.PolicyDocument;
import com.acme.hrassistant.domain.PolicyStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PolicyDocumentDto(
        UUID id,
        String name,
        String sourceFileName,
        String contentType,
        PolicyStatus status,
        LocalDate effectiveDate,
        String grade,
        String location,
        Long uploadedBy,
        Instant createdAt,
        String lastError,
        /** Present on get-by-id so the UI can open a clause / section. */
        String content
) {
    public static PolicyDocumentDto from(PolicyDocument doc) {
        return from(doc, false);
    }

    public static PolicyDocumentDto fromWithContent(PolicyDocument doc) {
        return from(doc, true);
    }

    private static PolicyDocumentDto from(PolicyDocument doc, boolean includeContent) {
        return new PolicyDocumentDto(
                doc.getId(),
                doc.getName(),
                doc.getSourceFileName(),
                doc.getContentType(),
                doc.getStatus(),
                doc.getEffectiveDate(),
                doc.getGrade(),
                doc.getLocation(),
                doc.getUploadedBy(),
                doc.getCreatedAt(),
                doc.getLastError(),
                includeContent ? doc.getContent() : null);
    }
}

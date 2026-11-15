package com.acme.hrassistant.web.dto;

import java.time.Instant;

public record PolicyBreachDto(
        Long id,
        Long employeeId,
        String employeeName,
        Long reportedById,
        String reportedByName,
        String policyDocumentId,
        String policyName,
        String notes,
        String severity,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}

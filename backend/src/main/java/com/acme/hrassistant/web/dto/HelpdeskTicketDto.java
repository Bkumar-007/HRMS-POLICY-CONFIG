package com.acme.hrassistant.web.dto;

import java.time.Instant;

public record HelpdeskTicketDto(
        Long id,
        Long employeeId,
        String employeeName,
        String subject,
        String description,
        String status,
        Instant createdAt
) {
}

package com.acme.hrassistant.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LeaveRequestDto(
        Long id,
        Long employeeId,
        String employeeName,
        String leaveType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal days,
        String reason,
        String status,
        Long reviewerId,
        String reviewerName,
        String reviewNote,
        Instant reviewedAt,
        Instant createdAt
) {
}

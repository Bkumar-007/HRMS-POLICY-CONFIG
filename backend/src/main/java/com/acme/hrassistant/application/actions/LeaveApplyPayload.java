package com.acme.hrassistant.application.actions;

import com.acme.hrassistant.domain.LeaveType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveApplyPayload(
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal days,
        String reason,
        boolean medicalCertificateProvided,
        String employeeLocation) {
}

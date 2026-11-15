package com.acme.hrassistant.application.leave;

import com.acme.hrassistant.domain.LeaveType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Pure Java leave validation (not LLM). Mirrors seed leave-policy.md rules.
 */
@Component
public class LeaveRules {

    public static final int MAX_CONSECUTIVE_CASUAL = 3;
    public static final int SICK_CERT_THRESHOLD_DAYS = 2;
    public static final int EARNED_NOTICE_WORKING_DAYS = 3;

    public record ApplyLeaveRequest(
            LeaveType leaveType,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal days,
            String reason,
            boolean medicalCertificateProvided) {
    }

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult ok() {
            return new ValidationResult(true, "OK");
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
    }

    public LeaveType parseLeaveType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("leaveType is required");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return switch (normalized) {
            case "CASUAL", "CL", "CASUAL_LEAVE" -> LeaveType.CASUAL;
            case "SICK", "SL", "SICK_LEAVE" -> LeaveType.SICK;
            case "EARNED", "EL", "EARNED_LEAVE", "PRIVILEGE", "PL" -> LeaveType.EARNED;
            default -> throw new IllegalArgumentException("Unknown leave type: " + raw);
        };
    }

    public BigDecimal calculateDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return BigDecimal.valueOf(days);
    }

    public ValidationResult validate(
            ApplyLeaveRequest request,
            BigDecimal remainingBalance,
            Set<LocalDate> holidaysOnRange,
            LocalDate today) {
        if (request.leaveType() == null) {
            return ValidationResult.fail("Leave type is required");
        }
        if (request.startDate() == null || request.endDate() == null) {
            return ValidationResult.fail("Start and end dates are required");
        }
        if (request.endDate().isBefore(request.startDate())) {
            return ValidationResult.fail("End date must be on or after start date");
        }
        if (request.startDate().isBefore(today)) {
            return ValidationResult.fail("Cannot apply leave for a past date");
        }
        if (request.days() == null || request.days().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.fail("Leave days must be positive");
        }
        if (remainingBalance == null || remainingBalance.compareTo(request.days()) < 0) {
            return ValidationResult.fail("Insufficient " + request.leaveType() + " balance");
        }
        if (holidaysOnRange != null && !holidaysOnRange.isEmpty()) {
            return ValidationResult.fail(
                    "Leave cannot be applied on a company holiday: " + holidaysOnRange.iterator().next());
        }

        long inclusiveDays = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (request.leaveType() == LeaveType.CASUAL && inclusiveDays > MAX_CONSECUTIVE_CASUAL) {
            return ValidationResult.fail(
                    "Casual leave cannot exceed " + MAX_CONSECUTIVE_CASUAL
                            + " consecutive days without manager escalation");
        }
        if (request.leaveType() == LeaveType.SICK
                && inclusiveDays > SICK_CERT_THRESHOLD_DAYS
                && !request.medicalCertificateProvided()) {
            return ValidationResult.fail(
                    "Medical certificate required for sick leave longer than "
                            + SICK_CERT_THRESHOLD_DAYS + " consecutive days");
        }
        if (request.leaveType() == LeaveType.EARNED) {
            int workingNotice = workingDaysBetween(today, request.startDate());
            if (workingNotice < EARNED_NOTICE_WORKING_DAYS) {
                return ValidationResult.fail(
                        "Earned leave requires " + EARNED_NOTICE_WORKING_DAYS
                                + " working days notice (got " + workingNotice + ")");
            }
            if (isMarchYearEndBlackout(request.startDate()) || isMarchYearEndBlackout(request.endDate())) {
                return ValidationResult.fail(
                        "Earned leave during March year-end blackout requires HR_ADMIN approval");
            }
        }
        return ValidationResult.ok();
    }

    /** Last 5 working days of March (year-end closing blackout for EL). */
    public boolean isMarchYearEndBlackout(LocalDate date) {
        if (date.getMonthValue() != 3) {
            return false;
        }
        LocalDate cursor = LocalDate.of(date.getYear(), 3, 31);
        int remaining = 5;
        while (remaining > 0) {
            if (!(cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                if (cursor.equals(date)) {
                    return true;
                }
                remaining--;
            }
            cursor = cursor.minusDays(1);
            if (cursor.getMonthValue() != 3) {
                break;
            }
        }
        return false;
    }

    /** Working days strictly after {@code from} up to and including {@code to}. */
    public int workingDaysBetween(LocalDate from, LocalDate to) {
        if (!to.isAfter(from)) {
            return 0;
        }
        int count = 0;
        LocalDate cursor = from.plusDays(1);
        while (!cursor.isAfter(to)) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }
}

package com.acme.hrassistant.application.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.hrassistant.application.leave.LeaveRules.ApplyLeaveRequest;
import com.acme.hrassistant.domain.LeaveType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeaveRulesTest {

    private LeaveRules rules;

    @BeforeEach
    void setUp() {
        rules = new LeaveRules();
    }

    @Test
    void parseLeaveTypeAliases() {
        assertEquals(LeaveType.CASUAL, rules.parseLeaveType("CL"));
        assertEquals(LeaveType.SICK, rules.parseLeaveType("sick leave"));
        assertEquals(LeaveType.EARNED, rules.parseLeaveType("EL"));
    }

    @Test
    void rejectsInsufficientBalance() {
        ApplyLeaveRequest request = new ApplyLeaveRequest(
                LeaveType.CASUAL,
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 20),
                BigDecimal.ONE,
                "personal",
                false);
        var result = rules.validate(request, BigDecimal.ZERO, Set.of(), LocalDate.of(2026, 11, 15));
        assertFalse(result.valid());
        assertTrue(result.message().contains("Insufficient"));
    }

    @Test
    void rejectsCasualOverThreeConsecutiveDays() {
        ApplyLeaveRequest request = new ApplyLeaveRequest(
                LeaveType.CASUAL,
                LocalDate.of(2026, 11, 16),
                LocalDate.of(2026, 11, 19),
                BigDecimal.valueOf(4),
                "trip",
                false);
        var result = rules.validate(request, BigDecimal.TEN, Set.of(), LocalDate.of(2026, 11, 10));
        assertFalse(result.valid());
        assertTrue(result.message().contains("consecutive"));
    }

    @Test
    void requiresMedicalCertificateForLongSickLeave() {
        ApplyLeaveRequest request = new ApplyLeaveRequest(
                LeaveType.SICK,
                LocalDate.of(2026, 11, 16),
                LocalDate.of(2026, 11, 19),
                BigDecimal.valueOf(4),
                "flu",
                false);
        var result = rules.validate(request, BigDecimal.TEN, Set.of(), LocalDate.of(2026, 11, 10));
        assertFalse(result.valid());
        assertTrue(result.message().toLowerCase().contains("medical"));
    }

    @Test
    void rejectsHolidayOverlap() {
        LocalDate holiday = LocalDate.of(2026, 11, 16);
        ApplyLeaveRequest request = new ApplyLeaveRequest(
                LeaveType.CASUAL,
                holiday,
                holiday,
                BigDecimal.ONE,
                "off",
                false);
        var result = rules.validate(request, BigDecimal.TEN, Set.of(holiday), LocalDate.of(2026, 11, 10));
        assertFalse(result.valid());
        assertTrue(result.message().contains("holiday"));
    }

    @Test
    void earnedLeaveRequiresWorkingDayNotice() {
        // Friday apply for next Monday = 1 working day notice
        LocalDate friday = LocalDate.of(2026, 11, 13); // Friday
        LocalDate monday = LocalDate.of(2026, 11, 16); // Monday
        ApplyLeaveRequest request = new ApplyLeaveRequest(
                LeaveType.EARNED,
                monday,
                monday,
                BigDecimal.ONE,
                "planned",
                false);
        var result = rules.validate(request, BigDecimal.TEN, Set.of(), friday);
        assertFalse(result.valid());
        assertTrue(result.message().contains("working days notice"));
    }

    @Test
    void acceptsValidCasualLeave() {
        ApplyLeaveRequest request = new ApplyLeaveRequest(
                LeaveType.CASUAL,
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 20),
                BigDecimal.ONE,
                "errand",
                false);
        var result = rules.validate(request, BigDecimal.TEN, Set.of(), LocalDate.of(2026, 11, 15));
        assertTrue(result.valid());
    }

    @Test
    void marchBlackoutDetectedForEarnedLeave() {
        // 2026-03-31 is Tuesday — within last 5 working days of March
        assertTrue(rules.isMarchYearEndBlackout(LocalDate.of(2026, 3, 31)));
        assertFalse(rules.isMarchYearEndBlackout(LocalDate.of(2026, 3, 10)));
    }
}

package com.acme.hrassistant.application.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.hrassistant.application.leave.LeaveRules.ApplyLeaveRequest;
import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.EmployeeRepository;
import com.acme.hrassistant.domain.LeaveBalanceRepository;
import com.acme.hrassistant.domain.LeaveType;
import com.acme.hrassistant.domain.PendingAction;
import com.acme.hrassistant.domain.PendingActionStatus;
import com.acme.hrassistant.support.PostgresTestcontainers;
import com.acme.hrassistant.support.TestAiConfig;
import com.acme.hrassistant.web.dto.ActionResultDto;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestAiConfig.class)
class PendingActionIntegrationTest extends PostgresTestcontainers {

    @Autowired
    PendingActionService pendingActionService;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    LeaveBalanceRepository leaveBalanceRepository;

    @Test
    void confirmApplyLeaveIsIdempotentAndDeductsBalanceOnce() {
        Employee priya = employeeRepository.findByUsername("priya.employee").orElseThrow();
        LocalDate start = nextWeekday(LocalDate.now().plusDays(2));
        BigDecimal before = leaveBalanceRepository
                .findByEmployeeIdAndYear(priya.getId(), LocalDate.now().getYear())
                .stream()
                .filter(b -> b.getLeaveType() == LeaveType.CASUAL)
                .findFirst()
                .orElseThrow()
                .getRemainingDays();

        ApplyLeaveRequest request = new ApplyLeaveRequest(
                LeaveType.CASUAL, start, start, BigDecimal.ONE, "demo", false);
        PendingAction pending = pendingActionService.createLeavePending(
                priya.getId(), "conv-test-1", priya.getLocation(), request);

        ActionResultDto first = pendingActionService.confirm(priya.getId(), pending.getId());
        ActionResultDto second = pendingActionService.confirm(priya.getId(), pending.getId());

        assertEquals(PendingActionStatus.CONFIRMED.name(), first.status());
        assertEquals(PendingActionStatus.CONFIRMED.name(), second.status());
        assertTrue(second.message().toLowerCase().contains("already"));

        BigDecimal after = leaveBalanceRepository
                .findByEmployeeIdAndYear(priya.getId(), LocalDate.now().getYear())
                .stream()
                .filter(b -> b.getLeaveType() == LeaveType.CASUAL)
                .findFirst()
                .orElseThrow()
                .getRemainingDays();
        assertEquals(0, before.subtract(BigDecimal.ONE).compareTo(after));
    }

    @Test
    void foreignPendingActionIsDenied() {
        Employee priya = employeeRepository.findByUsername("priya.employee").orElseThrow();
        Employee rahul = employeeRepository.findByUsername("rahul.employee").orElseThrow();
        LocalDate start = nextWeekday(LocalDate.now().plusDays(3));

        PendingAction pending = pendingActionService.createLeavePending(
                priya.getId(),
                "conv-test-2",
                priya.getLocation(),
                new ApplyLeaveRequest(LeaveType.CASUAL, start, start, BigDecimal.ONE, "x", false));

        assertThrows(
                AccessDeniedException.class,
                () -> pendingActionService.confirm(rahul.getId(), pending.getId()));
    }

    @Test
    void cancelThenConfirmFails() {
        Employee priya = employeeRepository.findByUsername("priya.employee").orElseThrow();
        LocalDate start = nextWeekday(LocalDate.now().plusDays(4));
        PendingAction pending = pendingActionService.createLeavePending(
                priya.getId(),
                "conv-test-3",
                priya.getLocation(),
                new ApplyLeaveRequest(LeaveType.CASUAL, start, start, BigDecimal.ONE, "x", false));

        pendingActionService.cancel(priya.getId(), pending.getId());
        assertThrows(
                IllegalStateException.class,
                () -> pendingActionService.confirm(priya.getId(), pending.getId()));
    }

    private static LocalDate nextWeekday(LocalDate from) {
        LocalDate d = from;
        while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.plusDays(1);
        }
        return d;
    }
}

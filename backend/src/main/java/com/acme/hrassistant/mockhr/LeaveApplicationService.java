package com.acme.hrassistant.mockhr;

import com.acme.hrassistant.application.leave.LeaveRules;
import com.acme.hrassistant.application.leave.LeaveRules.ApplyLeaveRequest;
import com.acme.hrassistant.application.leave.LeaveRules.ValidationResult;
import com.acme.hrassistant.domain.Holiday;
import com.acme.hrassistant.domain.HolidayRepository;
import com.acme.hrassistant.domain.LeaveBalance;
import com.acme.hrassistant.domain.LeaveBalanceRepository;
import com.acme.hrassistant.domain.LeaveRequest;
import com.acme.hrassistant.domain.LeaveRequestRepository;
import com.acme.hrassistant.domain.LeaveType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveApplicationService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveRules leaveRules;
    private final HolidayService holidayService;

    public LeaveApplicationService(
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveRequestRepository leaveRequestRepository,
            HolidayRepository holidayRepository,
            LeaveRules leaveRules,
            HolidayService holidayService) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.holidayRepository = holidayRepository;
        this.leaveRules = leaveRules;
        this.holidayService = holidayService;
    }

    @Transactional(readOnly = true)
    public ValidationResult validateApply(
            Long employeeId,
            String employeeLocation,
            ApplyLeaveRequest request,
            LocalDate today) {
        BigDecimal remaining = remainingBalance(employeeId, request.leaveType());
        Set<LocalDate> holidays = holidaysOnRange(request.startDate(), request.endDate(), employeeLocation);
        return leaveRules.validate(request, remaining, holidays, today);
    }

    @Transactional
    public LeaveRequest apply(
            Long employeeId,
            String employeeLocation,
            ApplyLeaveRequest request,
            String pendingActionId,
            LocalDate today) {
        ValidationResult validation = validateApply(employeeId, employeeLocation, request, today);
        if (!validation.valid()) {
            throw new IllegalStateException(validation.message());
        }

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, Year.now().getValue())
                .stream()
                .filter(b -> b.getLeaveType() == request.leaveType())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No leave balance for " + request.leaveType()));

        balance.setRemainingDays(balance.getRemainingDays().subtract(request.days()));
        leaveBalanceRepository.save(balance);

        LeaveRequest leaveRequest = new LeaveRequest(
                employeeId,
                request.leaveType(),
                request.startDate(),
                request.endDate(),
                request.days(),
                request.reason(),
                pendingActionId);
        return leaveRequestRepository.save(leaveRequest);
    }

    @Transactional(readOnly = true)
    public Optional<LeaveRequest> findByPendingActionId(String pendingActionId) {
        return leaveRequestRepository.findByPendingActionId(pendingActionId);
    }

    @Transactional(readOnly = true)
    public BigDecimal remainingBalance(Long employeeId, LeaveType leaveType) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, Year.now().getValue()).stream()
                .filter(b -> b.getLeaveType() == leaveType)
                .map(LeaveBalance::getRemainingDays)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private Set<LocalDate> holidaysOnRange(LocalDate start, LocalDate end, String location) {
        List<Holiday> holidays = holidayRepository.findForRangeAndLocation(start, end.plusDays(1), location);
        Set<LocalDate> dates = holidays.stream().map(Holiday::getDate).collect(Collectors.toCollection(HashSet::new));
        // Also cross-check via service (month may span) — keep repository query as source of truth
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            List<Holiday> monthHits = holidayService.getHolidays(d.getYear(), d.getMonthValue(), location);
            for (Holiday h : monthHits) {
                if (!h.getDate().isBefore(start) && !h.getDate().isAfter(end)) {
                    dates.add(h.getDate());
                }
            }
        }
        return dates;
    }
}

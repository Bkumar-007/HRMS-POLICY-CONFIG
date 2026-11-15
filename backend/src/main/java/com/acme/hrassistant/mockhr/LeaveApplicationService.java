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
import com.acme.hrassistant.domain.LeaveRequestStatus;
import com.acme.hrassistant.domain.LeaveType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LeaveApplicationService {

    private static final Set<LeaveRequestStatus> AWAITING =
            EnumSet.of(LeaveRequestStatus.PENDING_APPROVAL, LeaveRequestStatus.SUBMITTED);

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
        BigDecimal remaining = availableBalance(employeeId, request.leaveType());
        Set<LocalDate> holidays = holidaysOnRange(request.startDate(), request.endDate(), employeeLocation);
        return leaveRules.validate(request, remaining, holidays, today);
    }

    /**
     * Employee confirmation creates a leave request awaiting manager approval.
     * Balance is deducted only when approved.
     */
    @Transactional
    public LeaveRequest submit(
            Long employeeId,
            String employeeLocation,
            ApplyLeaveRequest request,
            String pendingActionId,
            LocalDate today) {
        ValidationResult validation = validateApply(employeeId, employeeLocation, request, today);
        if (!validation.valid()) {
            throw new IllegalStateException(validation.message());
        }

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

    /** @deprecated use {@link #submit} — kept for any callers expecting apply() */
    @Transactional
    public LeaveRequest apply(
            Long employeeId,
            String employeeLocation,
            ApplyLeaveRequest request,
            String pendingActionId,
            LocalDate today) {
        return submit(employeeId, employeeLocation, request, pendingActionId, today);
    }

    @Transactional
    public LeaveRequest approve(LeaveRequest leaveRequest, Long reviewerId, String note, String employeeLocation) {
        if (!leaveRequest.isAwaitingApproval()) {
            throw new IllegalStateException("Leave request is not awaiting approval");
        }
        // Use actual remaining balance (this pending request is not deducted yet).
        BigDecimal remaining = remainingBalance(leaveRequest.getEmployeeId(), leaveRequest.getLeaveType());
        if (remaining.compareTo(leaveRequest.getDays()) < 0) {
            throw new IllegalStateException(
                    "Insufficient leave balance to approve (" + remaining + " day(s) remaining).");
        }
        ApplyLeaveRequest request = new ApplyLeaveRequest(
                leaveRequest.getLeaveType(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getDays(),
                leaveRequest.getReason(),
                false);
        Set<LocalDate> holidays =
                holidaysOnRange(leaveRequest.getStartDate(), leaveRequest.getEndDate(), employeeLocation);
        ValidationResult validation = leaveRules.validate(request, remaining, holidays, LocalDate.now());
        if (!validation.valid()) {
            throw new IllegalStateException(validation.message());
        }

        LeaveBalance balance = requireBalance(leaveRequest.getEmployeeId(), leaveRequest.getLeaveType());
        balance.setRemainingDays(balance.getRemainingDays().subtract(leaveRequest.getDays()));
        leaveBalanceRepository.save(balance);

        leaveRequest.approve(reviewerId, blankToNull(note));
        return leaveRequestRepository.save(leaveRequest);
    }

    @Transactional
    public LeaveRequest reject(LeaveRequest leaveRequest, Long reviewerId, String note) {
        leaveRequest.reject(reviewerId, StringUtils.hasText(note) ? note.trim() : "Rejected by manager");
        return leaveRequestRepository.save(leaveRequest);
    }

    @Transactional
    public LeaveRequest cancelByEmployee(LeaveRequest leaveRequest) {
        leaveRequest.cancel();
        return leaveRequestRepository.save(leaveRequest);
    }

    @Transactional(readOnly = true)
    public Optional<LeaveRequest> findByPendingActionId(String pendingActionId) {
        return leaveRequestRepository.findByPendingActionId(pendingActionId);
    }

    @Transactional(readOnly = true)
    public Optional<LeaveRequest> findById(Long id) {
        return leaveRequestRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public BigDecimal remainingBalance(Long employeeId, LeaveType leaveType) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, Year.now().getValue()).stream()
                .filter(b -> b.getLeaveType() == leaveType)
                .map(LeaveBalance::getRemainingDays)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Balance available for new requests = remaining days minus days already pending approval.
     */
    @Transactional(readOnly = true)
    public BigDecimal availableBalance(Long employeeId, LeaveType leaveType) {
        BigDecimal remaining = remainingBalance(employeeId, leaveType);
        BigDecimal pendingDays = leaveRequestRepository
                .findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .filter(r -> AWAITING.contains(r.getStatus()) && r.getLeaveType() == leaveType)
                .map(LeaveRequest::getDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return remaining.subtract(pendingDays);
    }

    private LeaveBalance requireBalance(Long employeeId, LeaveType leaveType) {
        return leaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, Year.now().getValue())
                .stream()
                .filter(b -> b.getLeaveType() == leaveType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No leave balance for " + leaveType));
    }

    private Set<LocalDate> holidaysOnRange(LocalDate start, LocalDate end, String location) {
        List<Holiday> holidays = holidayRepository.findForRangeAndLocation(start, end.plusDays(1), location);
        Set<LocalDate> dates = holidays.stream().map(Holiday::getDate).collect(Collectors.toCollection(HashSet::new));
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

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

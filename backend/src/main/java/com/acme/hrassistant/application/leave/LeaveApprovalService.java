package com.acme.hrassistant.application.leave;

import com.acme.hrassistant.application.people.PeopleService;
import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.EmployeeRepository;
import com.acme.hrassistant.domain.LeaveRequest;
import com.acme.hrassistant.domain.LeaveRequestRepository;
import com.acme.hrassistant.domain.LeaveRequestStatus;
import com.acme.hrassistant.domain.Role;
import com.acme.hrassistant.mockhr.LeaveApplicationService;
import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.LeaveDecisionRequest;
import com.acme.hrassistant.web.dto.LeaveRequestDto;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveApprovalService {

    private static final Set<LeaveRequestStatus> AWAITING =
            EnumSet.of(LeaveRequestStatus.PENDING_APPROVAL, LeaveRequestStatus.SUBMITTED);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveApplicationService leaveApplicationService;
    private final EmployeeRepository employeeRepository;
    private final PeopleService peopleService;

    public LeaveApprovalService(
            LeaveRequestRepository leaveRequestRepository,
            LeaveApplicationService leaveApplicationService,
            EmployeeRepository employeeRepository,
            PeopleService peopleService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveApplicationService = leaveApplicationService;
        this.employeeRepository = employeeRepository;
        this.peopleService = peopleService;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> list(EmployeePrincipal viewer, boolean pendingOnly) {
        List<LeaveRequest> requests;
        if (viewer.getRole() == Role.HR_ADMIN) {
            requests = pendingOnly
                    ? leaveRequestRepository.findByStatusInOrderByCreatedAtDesc(AWAITING)
                    : leaveRequestRepository.findAllByOrderByCreatedAtDesc();
        } else if (viewer.getRole() == Role.MANAGER) {
            List<Long> teamIds = teamIds(viewer.getEmployeeId());
            if (teamIds.isEmpty()) {
                requests = List.of();
            } else if (pendingOnly) {
                requests = leaveRequestRepository.findByEmployeeIdInAndStatusInOrderByCreatedAtDesc(teamIds, AWAITING);
            } else {
                requests = leaveRequestRepository.findByEmployeeIdInOrderByCreatedAtDesc(teamIds);
            }
        } else {
            requests = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(viewer.getEmployeeId());
            if (pendingOnly) {
                requests = requests.stream().filter(r -> AWAITING.contains(r.getStatus())).toList();
            }
        }
        return toDtos(requests);
    }

    @Transactional
    public LeaveRequestDto approve(EmployeePrincipal actor, Long leaveId, LeaveDecisionRequest request) {
        peopleService.requireManagerOrHr(actor);
        LeaveRequest leave = loadForReview(actor, leaveId);
        Employee employee = employeeRepository
                .findById(leave.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        LeaveRequest updated = leaveApplicationService.approve(
                leave,
                actor.getEmployeeId(),
                request == null ? null : request.note(),
                employee.getLocation());
        return toDto(updated, nameMap());
    }

    @Transactional
    public LeaveRequestDto reject(EmployeePrincipal actor, Long leaveId, LeaveDecisionRequest request) {
        peopleService.requireManagerOrHr(actor);
        LeaveRequest leave = loadForReview(actor, leaveId);
        String note = request == null ? null : request.note();
        LeaveRequest updated = leaveApplicationService.reject(leave, actor.getEmployeeId(), note);
        return toDto(updated, nameMap());
    }

    @Transactional
    public LeaveRequestDto cancelOwn(EmployeePrincipal actor, Long leaveId) {
        LeaveRequest leave = leaveRequestRepository
                .findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!Objects.equals(leave.getEmployeeId(), actor.getEmployeeId())) {
            throw new AccessDeniedException("You can only cancel your own leave request");
        }
        return toDto(leaveApplicationService.cancelByEmployee(leave), nameMap());
    }

    private LeaveRequest loadForReview(EmployeePrincipal actor, Long leaveId) {
        LeaveRequest leave = leaveRequestRepository
                .findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!peopleService.canManageEmployee(actor, leave.getEmployeeId())) {
            throw new AccessDeniedException("You can only review leave for your team");
        }
        return leave;
    }

    private List<Long> teamIds(Long managerId) {
        return employeeRepository.findByManagerIdOrderByDisplayNameAsc(managerId).stream()
                .map(Employee::getId)
                .toList();
    }

    private List<LeaveRequestDto> toDtos(List<LeaveRequest> requests) {
        Map<Long, String> names = nameMap();
        return requests.stream().map(r -> toDto(r, names)).toList();
    }

    private LeaveRequestDto toDto(LeaveRequest leave, Map<Long, String> names) {
        return new LeaveRequestDto(
                leave.getId(),
                leave.getEmployeeId(),
                names.getOrDefault(leave.getEmployeeId(), "Unknown"),
                leave.getLeaveType().name(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getDays(),
                leave.getReason(),
                leave.getStatus().name(),
                leave.getReviewerId(),
                leave.getReviewerId() == null ? null : names.get(leave.getReviewerId()),
                leave.getReviewNote(),
                leave.getReviewedAt(),
                leave.getCreatedAt());
    }

    private Map<Long, String> nameMap() {
        return employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getDisplayName, (a, b) -> a));
    }
}

package com.acme.hrassistant.application.breaches;

import com.acme.hrassistant.application.people.PeopleService;
import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.EmployeeRepository;
import com.acme.hrassistant.domain.PolicyBreach;
import com.acme.hrassistant.domain.PolicyBreachRepository;
import com.acme.hrassistant.domain.PolicyBreachSeverity;
import com.acme.hrassistant.domain.PolicyBreachStatus;
import com.acme.hrassistant.domain.Role;
import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.CreatePolicyBreachRequest;
import com.acme.hrassistant.web.dto.PolicyBreachDto;
import com.acme.hrassistant.web.dto.UpdateBreachStatusRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyBreachService {

    private final PolicyBreachRepository policyBreachRepository;
    private final EmployeeRepository employeeRepository;
    private final PeopleService peopleService;

    public PolicyBreachService(
            PolicyBreachRepository policyBreachRepository,
            EmployeeRepository employeeRepository,
            PeopleService peopleService) {
        this.policyBreachRepository = policyBreachRepository;
        this.employeeRepository = employeeRepository;
        this.peopleService = peopleService;
    }

    @Transactional(readOnly = true)
    public List<PolicyBreachDto> list(EmployeePrincipal viewer) {
        List<PolicyBreach> breaches;
        if (viewer.getRole() == Role.HR_ADMIN) {
            breaches = policyBreachRepository.findAllByOrderByCreatedAtDesc();
        } else if (viewer.getRole() == Role.MANAGER) {
            List<Long> teamIds = employeeRepository
                    .findByManagerIdOrderByDisplayNameAsc(viewer.getEmployeeId())
                    .stream()
                    .map(Employee::getId)
                    .toList();
            breaches = teamIds.isEmpty()
                    ? List.of()
                    : policyBreachRepository.findByEmployeeIdInOrderByCreatedAtDesc(teamIds);
        } else {
            breaches = policyBreachRepository.findByEmployeeIdOrderByCreatedAtDesc(viewer.getEmployeeId());
        }
        return toDtos(breaches);
    }

    @Transactional
    public PolicyBreachDto create(EmployeePrincipal actor, CreatePolicyBreachRequest request) {
        peopleService.requireManagerOrHr(actor);
        if (!peopleService.canManageEmployee(actor, request.employeeId())) {
            throw new AccessDeniedException("You can only flag breaches for your team.");
        }
        employeeRepository
                .findById(request.employeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));

        PolicyBreachSeverity severity = parseSeverity(request.severity());
        PolicyBreach breach = new PolicyBreach(
                request.employeeId(),
                actor.getEmployeeId(),
                blankToNull(request.policyDocumentId()),
                request.policyName().trim(),
                request.notes().trim(),
                severity);
        return toDto(policyBreachRepository.save(breach), nameMap());
    }

    @Transactional
    public PolicyBreachDto updateStatus(
            EmployeePrincipal actor, Long breachId, UpdateBreachStatusRequest request) {
        peopleService.requireManagerOrHr(actor);
        PolicyBreach breach = policyBreachRepository
                .findById(breachId)
                .orElseThrow(() -> new IllegalArgumentException("Breach not found."));
        if (!peopleService.canManageEmployee(actor, breach.getEmployeeId())
                && !Objects.equals(breach.getReportedById(), actor.getEmployeeId())) {
            throw new AccessDeniedException("Not allowed to update this breach.");
        }
        breach.setStatus(parseStatus(request.status()));
        return toDto(policyBreachRepository.save(breach), nameMap());
    }

    private List<PolicyBreachDto> toDtos(List<PolicyBreach> breaches) {
        Map<Long, String> names = nameMap();
        return breaches.stream().map(b -> toDto(b, names)).toList();
    }

    private PolicyBreachDto toDto(PolicyBreach breach, Map<Long, String> names) {
        return new PolicyBreachDto(
                breach.getId(),
                breach.getEmployeeId(),
                names.getOrDefault(breach.getEmployeeId(), "Unknown"),
                breach.getReportedById(),
                names.getOrDefault(breach.getReportedById(), "Unknown"),
                breach.getPolicyDocumentId(),
                breach.getPolicyName(),
                breach.getNotes(),
                breach.getSeverity().name(),
                breach.getStatus().name(),
                breach.getCreatedAt(),
                breach.getUpdatedAt());
    }

    private Map<Long, String> nameMap() {
        return employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getDisplayName, (a, b) -> a));
    }

    private static PolicyBreachSeverity parseSeverity(String value) {
        try {
            return PolicyBreachSeverity.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Severity must be LOW, MEDIUM, or HIGH.");
        }
    }

    private static PolicyBreachStatus parseStatus(String value) {
        try {
            return PolicyBreachStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Status must be OPEN, ACKNOWLEDGED, RESOLVED, or DISMISSED.");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

package com.acme.hrassistant.application.people;

import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.EmployeeRepository;
import com.acme.hrassistant.domain.PolicyBreachRepository;
import com.acme.hrassistant.domain.PolicyBreachStatus;
import com.acme.hrassistant.domain.Role;
import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.EmployeeDirectoryDto;
import com.acme.hrassistant.web.dto.UpdateManagerRequest;
import com.acme.hrassistant.web.dto.UpdateRoleRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PeopleService {

    private final EmployeeRepository employeeRepository;
    private final PolicyBreachRepository policyBreachRepository;

    public PeopleService(EmployeeRepository employeeRepository, PolicyBreachRepository policyBreachRepository) {
        this.employeeRepository = employeeRepository;
        this.policyBreachRepository = policyBreachRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDirectoryDto> directory(EmployeePrincipal viewer) {
        List<Employee> employees = visibleEmployees(viewer);
        return toDtos(employees);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDirectoryDto> team(EmployeePrincipal viewer) {
        requireManagerOrHr(viewer);
        List<Employee> reports = employeeRepository.findByManagerIdOrderByDisplayNameAsc(viewer.getEmployeeId());
        return toDtos(reports);
    }

    @Transactional
    public EmployeeDirectoryDto updateRole(EmployeePrincipal actor, Long employeeId, UpdateRoleRequest request) {
        requireHrAdmin(actor);
        if (Objects.equals(actor.getEmployeeId(), employeeId)) {
            throw new IllegalArgumentException("You cannot change your own role.");
        }
        Employee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
        Role role = parseRole(request.role());
        employee.setRole(role);
        return toDto(employeeRepository.save(employee), managerNames());
    }

    @Transactional
    public EmployeeDirectoryDto updateManager(
            EmployeePrincipal actor, Long employeeId, UpdateManagerRequest request) {
        requireHrAdmin(actor);
        Employee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
        Long managerId = request.managerId();
        if (managerId != null) {
            if (Objects.equals(managerId, employeeId)) {
                throw new IllegalArgumentException("An employee cannot manage themselves.");
            }
            Employee manager = employeeRepository
                    .findById(managerId)
                    .orElseThrow(() -> new IllegalArgumentException("Manager not found."));
            if (manager.getRole() == Role.EMPLOYEE) {
                throw new IllegalArgumentException("Assign MANAGER or HR_ADMIN before setting as manager.");
            }
        }
        employee.setManagerId(managerId);
        return toDto(employeeRepository.save(employee), managerNames());
    }

    public void requireManagerOrHr(EmployeePrincipal viewer) {
        if (viewer.getRole() != Role.HR_ADMIN && viewer.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Managers or HR admins only");
        }
    }

    public void requireHrAdmin(EmployeePrincipal viewer) {
        if (viewer.getRole() != Role.HR_ADMIN) {
            throw new AccessDeniedException("HR admins only");
        }
    }

    public boolean canManageEmployee(EmployeePrincipal actor, Long employeeId) {
        if (actor.getRole() == Role.HR_ADMIN) {
            return true;
        }
        if (actor.getRole() != Role.MANAGER) {
            return false;
        }
        return employeeRepository
                .findById(employeeId)
                .map(emp -> Objects.equals(emp.getManagerId(), actor.getEmployeeId()))
                .orElse(false);
    }

    private List<Employee> visibleEmployees(EmployeePrincipal viewer) {
        if (viewer.getRole() == Role.HR_ADMIN) {
            return employeeRepository.findAllByOrderByDisplayNameAsc();
        }
        if (viewer.getRole() == Role.MANAGER) {
            return employeeRepository.findByManagerIdOrderByDisplayNameAsc(viewer.getEmployeeId());
        }
        return employeeRepository
                .findById(viewer.getEmployeeId())
                .map(List::of)
                .orElse(List.of());
    }

    private List<EmployeeDirectoryDto> toDtos(List<Employee> employees) {
        Map<Long, String> managers = managerNames();
        return employees.stream().map(e -> toDto(e, managers)).toList();
    }

    private EmployeeDirectoryDto toDto(Employee employee, Map<Long, String> managers) {
        long openBreaches =
                policyBreachRepository.countByEmployeeIdAndStatus(employee.getId(), PolicyBreachStatus.OPEN);
        return new EmployeeDirectoryDto(
                employee.getId(),
                employee.getUsername(),
                employee.getDisplayName(),
                employee.getRole().name(),
                employee.getGrade(),
                employee.getLocation(),
                employee.getDepartment(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getManagerId(),
                employee.getManagerId() == null ? null : managers.get(employee.getManagerId()),
                employee.isActive(),
                openBreaches);
    }

    private Map<Long, String> managerNames() {
        return employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getDisplayName, (a, b) -> a));
    }

    private static Role parseRole(String role) {
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Role must be EMPLOYEE, MANAGER, or HR_ADMIN.");
        }
    }
}

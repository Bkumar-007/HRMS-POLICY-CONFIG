package com.acme.hrassistant.application.people;

import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.EmployeeRepository;
import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.EmployeeProfileDto;
import com.acme.hrassistant.web.dto.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProfileService {

    private final EmployeeRepository employeeRepository;

    public ProfileService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public EmployeeProfileDto getProfile(EmployeePrincipal viewer) {
        Employee employee = requireEmployee(viewer.getEmployeeId());
        return toDto(employee);
    }

    @Transactional
    public EmployeeProfileDto updateProfile(EmployeePrincipal viewer, UpdateProfileRequest request) {
        Employee employee = requireEmployee(viewer.getEmployeeId());

        if (request.displayName() != null) {
            String name = request.displayName().trim();
            if (!StringUtils.hasText(name)) {
                throw new IllegalArgumentException("Display name cannot be blank.");
            }
            employee.setDisplayName(name);
        }

        if (request.email() != null) {
            String email = request.email().trim();
            if (!StringUtils.hasText(email)) {
                throw new IllegalArgumentException("Email cannot be blank.");
            }
            employee.setEmail(email.toLowerCase());
        }

        if (request.phone() != null) {
            String phone = normalizePhone(request.phone());
            if (!StringUtils.hasText(phone)) {
                throw new IllegalArgumentException("Phone cannot be blank.");
            }
            if (phone.length() < 10) {
                throw new IllegalArgumentException("Enter a valid 10-digit mobile number.");
            }
            employee.setPhone(phone);
        }

        return toDto(employeeRepository.save(employee));
    }

    private Employee requireEmployee(Long id) {
        return employeeRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
    }

    private EmployeeProfileDto toDto(Employee employee) {
        String managerName = null;
        if (employee.getManagerId() != null) {
            managerName = employeeRepository
                    .findById(employee.getManagerId())
                    .map(Employee::getDisplayName)
                    .orElse(null);
        }
        return new EmployeeProfileDto(
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
                managerName);
    }

    private static String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        return digits;
    }
}

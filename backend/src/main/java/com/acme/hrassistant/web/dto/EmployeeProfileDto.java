package com.acme.hrassistant.web.dto;

public record EmployeeProfileDto(
        Long employeeId,
        String username,
        String displayName,
        String role,
        String grade,
        String location,
        String department,
        String email,
        String phone,
        Long managerId,
        String managerName
) {
}

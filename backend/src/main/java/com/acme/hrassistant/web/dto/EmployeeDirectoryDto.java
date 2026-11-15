package com.acme.hrassistant.web.dto;

public record EmployeeDirectoryDto(
        Long id,
        String username,
        String displayName,
        String role,
        String grade,
        String location,
        String department,
        String email,
        String phone,
        Long managerId,
        String managerName,
        boolean active,
        long openBreachCount
) {
}

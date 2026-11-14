package com.acme.hrassistant.web.dto;

public record LoginResponse(
        String token,
        Long employeeId,
        String username,
        String displayName,
        String role
) {
}
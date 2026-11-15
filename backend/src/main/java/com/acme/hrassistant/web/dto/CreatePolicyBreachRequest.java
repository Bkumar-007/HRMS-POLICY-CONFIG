package com.acme.hrassistant.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePolicyBreachRequest(
        @NotNull Long employeeId,
        String policyDocumentId,
        @NotBlank String policyName,
        @NotBlank String notes,
        @NotBlank String severity
) {
}

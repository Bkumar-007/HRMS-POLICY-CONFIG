package com.acme.hrassistant.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateBreachStatusRequest(@NotBlank String status) {
}

package com.acme.hrassistant.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTicketStatusRequest(@NotBlank String status) {
}

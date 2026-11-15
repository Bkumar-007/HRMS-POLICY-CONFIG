package com.acme.hrassistant.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 120) String displayName,
        @Email @Size(max = 120) String email,
        @Size(max = 20) String phone
) {
}

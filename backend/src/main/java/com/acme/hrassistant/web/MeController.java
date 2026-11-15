package com.acme.hrassistant.web;

import com.acme.hrassistant.application.people.ProfileService;
import com.acme.hrassistant.security.SecurityUtils;
import com.acme.hrassistant.web.dto.ApiResponse;
import com.acme.hrassistant.web.dto.EmployeeProfileDto;
import com.acme.hrassistant.web.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final ProfileService profileService;

    public MeController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<EmployeeProfileDto> me() {
        return ApiResponse.ok(profileService.getProfile(SecurityUtils.currentUser()));
    }

    @PutMapping("/profile")
    public ApiResponse<EmployeeProfileDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(profileService.updateProfile(SecurityUtils.currentUser(), request));
    }
}

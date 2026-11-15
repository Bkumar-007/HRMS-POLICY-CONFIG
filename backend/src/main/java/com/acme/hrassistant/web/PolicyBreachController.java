package com.acme.hrassistant.web;

import com.acme.hrassistant.application.breaches.PolicyBreachService;
import com.acme.hrassistant.security.SecurityUtils;
import com.acme.hrassistant.web.dto.ApiResponse;
import com.acme.hrassistant.web.dto.CreatePolicyBreachRequest;
import com.acme.hrassistant.web.dto.PolicyBreachDto;
import com.acme.hrassistant.web.dto.UpdateBreachStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/breaches")
public class PolicyBreachController {

    private final PolicyBreachService policyBreachService;

    public PolicyBreachController(PolicyBreachService policyBreachService) {
        this.policyBreachService = policyBreachService;
    }

    @GetMapping
    public ApiResponse<List<PolicyBreachDto>> list() {
        return ApiResponse.ok(policyBreachService.list(SecurityUtils.currentUser()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER')")
    public ApiResponse<PolicyBreachDto> create(@Valid @RequestBody CreatePolicyBreachRequest request) {
        return ApiResponse.ok(policyBreachService.create(SecurityUtils.currentUser(), request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER')")
    public ApiResponse<PolicyBreachDto> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateBreachStatusRequest request) {
        return ApiResponse.ok(policyBreachService.updateStatus(SecurityUtils.currentUser(), id, request));
    }
}

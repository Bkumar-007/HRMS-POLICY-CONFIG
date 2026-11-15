package com.acme.hrassistant.web;

import com.acme.hrassistant.application.people.PeopleService;
import com.acme.hrassistant.security.SecurityUtils;
import com.acme.hrassistant.web.dto.ApiResponse;
import com.acme.hrassistant.web.dto.EmployeeDirectoryDto;
import com.acme.hrassistant.web.dto.UpdateManagerRequest;
import com.acme.hrassistant.web.dto.UpdateRoleRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people")
public class PeopleController {

    private final PeopleService peopleService;

    public PeopleController(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    @GetMapping
    public ApiResponse<List<EmployeeDirectoryDto>> directory() {
        return ApiResponse.ok(peopleService.directory(SecurityUtils.currentUser()));
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER')")
    public ApiResponse<List<EmployeeDirectoryDto>> team() {
        return ApiResponse.ok(peopleService.team(SecurityUtils.currentUser()));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ApiResponse<EmployeeDirectoryDto> updateRole(
            @PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.ok(peopleService.updateRole(SecurityUtils.currentUser(), id, request));
    }

    @PutMapping("/{id}/manager")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ApiResponse<EmployeeDirectoryDto> updateManager(
            @PathVariable Long id, @Valid @RequestBody UpdateManagerRequest request) {
        return ApiResponse.ok(peopleService.updateManager(SecurityUtils.currentUser(), id, request));
    }
}

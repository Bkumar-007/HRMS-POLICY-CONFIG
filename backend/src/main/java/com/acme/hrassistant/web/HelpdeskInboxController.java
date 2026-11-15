package com.acme.hrassistant.web;

import com.acme.hrassistant.application.helpdesk.HelpdeskInboxService;
import com.acme.hrassistant.security.SecurityUtils;
import com.acme.hrassistant.web.dto.ApiResponse;
import com.acme.hrassistant.web.dto.HelpdeskTicketDto;
import com.acme.hrassistant.web.dto.UpdateTicketStatusRequest;
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
@RequestMapping("/api/helpdesk")
public class HelpdeskInboxController {

    private final HelpdeskInboxService helpdeskInboxService;

    public HelpdeskInboxController(HelpdeskInboxService helpdeskInboxService) {
        this.helpdeskInboxService = helpdeskInboxService;
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER')")
    public ApiResponse<List<HelpdeskTicketDto>> list() {
        return ApiResponse.ok(helpdeskInboxService.list(SecurityUtils.currentUser()));
    }

    @PutMapping("/tickets/{id}/status")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ApiResponse<HelpdeskTicketDto> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ApiResponse.ok(helpdeskInboxService.updateStatus(SecurityUtils.currentUser(), id, request));
    }
}

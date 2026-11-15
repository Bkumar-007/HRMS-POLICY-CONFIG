package com.acme.hrassistant.web;

import com.acme.hrassistant.application.actions.PendingActionService;
import com.acme.hrassistant.security.SecurityUtils;
import com.acme.hrassistant.web.dto.ActionResultDto;
import com.acme.hrassistant.web.dto.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actions")
public class ActionController {

    private final PendingActionService pendingActionService;

    public ActionController(PendingActionService pendingActionService) {
        this.pendingActionService = pendingActionService;
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<ActionResultDto> confirm(@PathVariable String id) {
        return ApiResponse.ok(pendingActionService.confirm(SecurityUtils.currentUser().getEmployeeId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<ActionResultDto> cancel(@PathVariable String id) {
        return ApiResponse.ok(pendingActionService.cancel(SecurityUtils.currentUser().getEmployeeId(), id));
    }
}

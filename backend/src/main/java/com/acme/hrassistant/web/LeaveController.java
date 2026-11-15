package com.acme.hrassistant.web;

import com.acme.hrassistant.application.leave.LeaveApprovalService;
import com.acme.hrassistant.security.SecurityUtils;
import com.acme.hrassistant.web.dto.ApiResponse;
import com.acme.hrassistant.web.dto.LeaveDecisionRequest;
import com.acme.hrassistant.web.dto.LeaveRequestDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveApprovalService leaveApprovalService;

    public LeaveController(LeaveApprovalService leaveApprovalService) {
        this.leaveApprovalService = leaveApprovalService;
    }

    @GetMapping("/requests")
    public ApiResponse<List<LeaveRequestDto>> list(
            @RequestParam(name = "pendingOnly", defaultValue = "false") boolean pendingOnly) {
        return ApiResponse.ok(leaveApprovalService.list(SecurityUtils.currentUser(), pendingOnly));
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER')")
    public ApiResponse<LeaveRequestDto> approve(
            @PathVariable Long id, @RequestBody(required = false) LeaveDecisionRequest request) {
        return ApiResponse.ok(
                leaveApprovalService.approve(SecurityUtils.currentUser(), id, request == null ? new LeaveDecisionRequest(null) : request));
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER')")
    public ApiResponse<LeaveRequestDto> reject(
            @PathVariable Long id, @RequestBody(required = false) LeaveDecisionRequest request) {
        return ApiResponse.ok(
                leaveApprovalService.reject(SecurityUtils.currentUser(), id, request == null ? new LeaveDecisionRequest(null) : request));
    }

    @PostMapping("/requests/{id}/cancel")
    public ApiResponse<LeaveRequestDto> cancel(@PathVariable Long id) {
        return ApiResponse.ok(leaveApprovalService.cancelOwn(SecurityUtils.currentUser(), id));
    }
}

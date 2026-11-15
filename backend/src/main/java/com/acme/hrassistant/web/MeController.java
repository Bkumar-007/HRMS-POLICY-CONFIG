package com.acme.hrassistant.web;

import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.security.SecurityUtils;
import com.acme.hrassistant.web.dto.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public ApiResponse<Map<String, Object>> me() {
        EmployeePrincipal user = SecurityUtils.currentUser();
        Map<String, Object> profile = Map.of(
                "employeeId", user.getEmployeeId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "role", user.getRole().name(),
                "grade", user.getGrade(),
                "location", user.getLocation());
        return ApiResponse.ok(profile);
    }
}

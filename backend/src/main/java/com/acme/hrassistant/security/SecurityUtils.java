package com.acme.hrassistant.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static EmployeePrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof EmployeePrincipal principal)) {
            throw new IllegalStateException("No authenticated employee in security context");
        }
        return principal;
    }
}
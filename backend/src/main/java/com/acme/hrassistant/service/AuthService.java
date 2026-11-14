package com.acme.hrassistant.service;

import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.security.JwtService;
import com.acme.hrassistant.web.dto.LoginRequest;
import com.acme.hrassistant.web.dto.LoginResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        return new LoginResponse(
                token,
                principal.getEmployeeId(),
                principal.getUsername(),
                principal.getDisplayName(),
                principal.getRole().name());
    }
}
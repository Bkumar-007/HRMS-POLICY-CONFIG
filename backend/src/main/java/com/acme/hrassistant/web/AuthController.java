package com.acme.hrassistant.web;

import com.acme.hrassistant.application.auth.AuthService;
import com.acme.hrassistant.application.auth.PasswordResetService;
import com.acme.hrassistant.web.dto.ApiResponse;
import com.acme.hrassistant.web.dto.ForgotPasswordRequest;
import com.acme.hrassistant.web.dto.ForgotPasswordResponse;
import com.acme.hrassistant.web.dto.LoginRequest;
import com.acme.hrassistant.web.dto.LoginResponse;
import com.acme.hrassistant.web.dto.ResetPasswordRequest;
import com.acme.hrassistant.web.dto.VerifyOtpRequest;
import com.acme.hrassistant.web.dto.VerifyOtpResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.ok(passwordResetService.forgotPassword(request));
    }

    @PostMapping("/verify-otp")
    public ApiResponse<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.ok(passwordResetService.verifyOtp(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ApiResponse.ok(Map.of(
                "reset", true,
                "message", "Password updated. You can sign in with your new password."));
    }
}

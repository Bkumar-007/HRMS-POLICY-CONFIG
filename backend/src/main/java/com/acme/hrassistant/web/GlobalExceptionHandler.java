package com.acme.hrassistant.web;

import com.acme.hrassistant.web.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String AI_UNAVAILABLE =
            "The AI model is busy or unavailable right now. Please try again in a moment.";

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUpload(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Uploaded file is too large");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({TransientAiException.class, NonTransientAiException.class})
    public ResponseEntity<ApiResponse<Void>> handleAiUnavailable(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, AI_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        if (isAiProviderFailure(ex)) {
            return build(HttpStatus.SERVICE_UNAVAILABLE, AI_UNAVAILABLE);
        }
        String message = ex.getMessage() == null ? "Internal server error" : ex.getMessage();
        return build(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    private boolean isAiProviderFailure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String name = current.getClass().getName();
            String message = current.getMessage() == null ? "" : current.getMessage();
            if (name.contains("genai")
                    || message.contains("high demand")
                    || message.contains("503")
                    || message.contains("429")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }
}

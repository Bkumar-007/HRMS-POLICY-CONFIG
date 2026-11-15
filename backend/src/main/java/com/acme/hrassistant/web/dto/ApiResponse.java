package com.acme.hrassistant.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API envelope.
 * {@code code}: 1 = success, 0 = failure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        int code,
        T data,
        String message,
        Long count
) {
    public static final int SUCCESS = 1;
    public static final int FAILURE = 0;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS, data, "Success", null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(SUCCESS, data, message, null);
    }

    public static <T> ApiResponse<T> ok(T data, long count) {
        return new ApiResponse<>(SUCCESS, data, "Success", count);
    }

    public static <T> ApiResponse<T> ok(T data, String message, long count) {
        return new ApiResponse<>(SUCCESS, data, message, count);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(FAILURE, null, message == null ? "Error" : message, null);
    }
}

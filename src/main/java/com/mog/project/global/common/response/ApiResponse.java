package com.mog.project.global.common.response;

public record ApiResponse<T>(
    int status,
    String code,
    String message,
    T data
) {
    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(200, code, message, data);
    }

    public static ApiResponse<Void> success(String code, String message) {
        return new ApiResponse<>(200, code, message, null);
    }

    public static ApiResponse<Void> failure(int status, String code, String message) {
        return new ApiResponse<>(status, code, message, null);
    }
}

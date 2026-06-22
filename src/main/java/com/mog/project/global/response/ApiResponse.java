package com.mog.project.global.response;

import com.mog.project.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final int status;
    private final String code;
    private final String message;
    private final T data;

    // 응답 성공 시
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, "SUCCESS", message, data);
    }

    // 응답 성공 시 (데이터 없이 메세지만)
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(200, "SUCCESS", message, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(), null);
    }




}

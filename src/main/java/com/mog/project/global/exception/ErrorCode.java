package com.mog.project.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_KAKAO_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_KAKAO_TOKEN", "유효하지 않은 카카오 토큰입니다."),
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_USER", "인증되지 않은 사용자입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND", "Refresh Token이 존재하지 않습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public int getStatus() {
        return httpStatus.value();
    }
}

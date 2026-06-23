package com.mog.project.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(400, "INVALID_INPUT", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),

    // 인증/인가
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(403, "FORBIDDEN", "접근 권한이 없습니다."),

    // 만남 기록
    RECORD_NOT_FOUND(404, "RECORD_NOT_FOUND", "해당 차수 기록을 찾을 수 없습니다."),
    INVALID_MEMBER(400, "INVALID_MEMBER", "방 멤버가 아닌 참여자가 포함되어 있습니다."),

    // 방
    ROOM_NOT_FOUND(404, "ROOM_NOT_FOUND", "존재하지 않는 방입니다."),

    // 사진
    PHOTO_NOT_FOUND(404, "PHOTO_NOT_FOUND", "존재하지 않는 사진입니다."),
    PHOTO_LIMIT_EXCEEDED(400, "PHOTO_LIMIT_EXCEEDED", "사진은 최대 3장까지 업로드할 수 있습니다."),
    INVALID_IMAGE(400, "INVALID_IMAGE", "지원하지 않는 이미지 형식입니다."),
    IMAGE_TOO_LARGE(400, "IMAGE_TOO_LARGE", "이미지 크기를 초과했습니다."),

    // OCR
    OCR_FAILED(422, "OCR_FAILED", "영수증을 인식할 수 없습니다."),
    OCR_SERVICE_ERROR(503, "OCR_SERVICE_ERROR", "OCR 서비스에 일시적인 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

}

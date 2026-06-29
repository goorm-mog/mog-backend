package com.mog.project.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // 공통
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력값이 올바르지 않습니다."),  
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
  
  
  // 인증 인가
  UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_USER", "인증되지 않은 사용자입니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),  
  INVALID_KAKAO_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_KAKAO_TOKEN", "유효하지 않은 카카오 토큰입니다."),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."),
  REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND", "Refresh Token이 존재하지 않습니다."),
    
  // 만남 기록                                                                  
  RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD_NOT_FOUND", "해당 차수 기록을 찾을 수 없습니다."),                                                              
  INVALID_MEMBER(HttpStatus.BAD_REQUEST, "INVALID_MEMBER", "방 멤버가 아닌 참여자가 포함되어 있습니다."),                                                    
  INVALID_PAYER(HttpStatus.BAD_REQUEST, "INVALID_PAYER", "결제자가 방 멤버가 아닙니다."),
  
  // 방                                                                         
  ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "존재하지 않는 방입니다."),                                                                      
   
  // 사진                                                                       
  PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "PHOTO_NOT_FOUND", "존재하지 않는 사진입니다."),                                                                    
  PHOTO_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "PHOTO_LIMIT_EXCEEDED", "사진은 최대 3장까지 업로드할 수 있습니다."),                                             
  INVALID_IMAGE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", "지원하지 않는 이미지 형식입니다."),                                                                    
  IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "IMAGE_TOO_LARGE", "이미지 크기를 초과했습니다."),                                                                  
                     
  // OCR                                                                        
  OCR_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "OCR_FAILED", "영수증을 인식할 수 없습니다."),                                                                      
  OCR_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "OCR_SERVICE_ERROR", "OCR 서비스에 일시적인 오류가 발생했습니다.");
  
  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  public int getStatus() {
    return httpStatus.value();
  }
}

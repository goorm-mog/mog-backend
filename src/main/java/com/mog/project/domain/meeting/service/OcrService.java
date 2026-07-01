package com.mog.project.domain.meeting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mog.project.domain.meeting.dto.response.OcrResponse;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import com.mog.project.global.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.Base64;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OcrService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // 받을 이미지 타입
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    // 요청 Client
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public OcrResponse analyzeReceipt(MultipartFile image) {
        validateImage(image);

        String base64 = toBase64(image);
        String json = geminiClient.analyzeReceipt(base64, image.getContentType());

        return parseResponse(json);
    }

    // 이미지 검증
    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) { // 이미지가 비어있거나 null일 때
            throw new GlobalException(ErrorCode.INVALID_IMAGE);
        }
        if (image.getSize() > MAX_FILE_SIZE) { // 이미지 사이즈가 너무 클때
            throw new GlobalException(ErrorCode.IMAGE_TOO_LARGE);
        }
    }

    private String toBase64(MultipartFile image) {
        try { // 이미지 인코딩을 진행
            return Base64.getEncoder().encodeToString(image.getBytes());
        } catch (IOException e) {
            throw new GlobalException(ErrorCode.OCR_SERVICE_ERROR);
        }
    }

    private OcrResponse parseResponse(String json) {
        try { // json 파싱을 진행, json 문자열을 OcrResponse 객체로 변환
            return objectMapper.readValue(json, OcrResponse.class);
        } catch (JsonProcessingException e) {
            return OcrResponse.empty(); // 영수증을 인식하지 못했을 때 빈 응답 반환
        }
    }

}

package com.mog.project.global.gemini;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record GeminiRequest(
        List<Content> contents,
        GenerationConfig generationConfig
) {
    // 하나의 대화 턴 (이미지 + 텍스트 묶음)
    public record Content(List<Part> parts) {}

    // 이미지 파트와 텍스트 파트 중 하나만 값이 들어감
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(String text, InlineData inlineData) {
        public static Part ofText(String text) {
            return new Part(text, null);
        }

        public static Part ofImage(String mimeType, String base64Data) {
            return new Part(null, new InlineData(mimeType, base64Data));
        }
    }

    public record InlineData(String mimeType, String data) {}

    public record GenerationConfig(String responseMimeType) {}
}

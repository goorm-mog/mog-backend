package com.mog.project.global.gemini;

import java.util.List;

public record GeminiResponse(List<Candidate> candidates) {

    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}

    // 첫 번째 후보의 텍스트 추출
    public String getText() {
        if (candidates == null || candidates.isEmpty()) return null;
        List<Part> parts = candidates.get(0).content().parts();
        if (parts == null || parts.isEmpty()) return null;
        return parts.get(0).text();
    }
}

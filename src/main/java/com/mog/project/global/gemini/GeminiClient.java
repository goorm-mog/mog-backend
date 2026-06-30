package com.mog.project.global.gemini;


import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;


@Component
public class GeminiClient {
    private static final String GEMINI_URL="https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent";

    private static final String PROMPT = """
            이 영수증 이미지에서 정보를 추출해주세요.
            반드시 아래 JSON 형식으로만 응답하세요:
            {
                "storeName": "가게명 (없으면 null)",
                "totalAmount": 총금액_숫자 (없으면 0),
                "items": [
                    {
                        "name": "메뉴명",
                        "price": 가격_숫자,
                        "count": 수량_숫자 (없으면 null)
                    }
                ]
            }
            영수증이 아니거나 인식할 수 없으면:
            {
                "storeName": null, "totalAmount": 0, "items": []
            }
            """;

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestClient restClient;

    public GeminiClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    // base64 이미지와 mimeType을 받아 Gemini로 분석을 요청, 응답 JSON 문자열 반환
    public String analyzeReceipt(String base64Image, String mimeType) {
        GeminiRequest request = new GeminiRequest(
                List.of(new GeminiRequest.Content(
                        List.of(
                                GeminiRequest.Part.ofImage(mimeType, base64Image),
                                GeminiRequest.Part.ofText(PROMPT)
                        )
                )),
                new GeminiRequest.GenerationConfig("application/json")
        );
        try {
            GeminiResponse response = restClient.post()
                    .uri(GEMINI_URL + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null || response.getText() == null) {
                throw new GlobalException(ErrorCode.OCR_SERVICE_ERROR);
            }
            return response.getText();
        } catch (RestClientException e) {
            throw new GlobalException(ErrorCode.OCR_SERVICE_ERROR);
        }
    }
}
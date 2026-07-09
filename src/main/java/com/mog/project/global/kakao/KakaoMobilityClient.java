package com.mog.project.global.kakao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
 
import java.math.BigDecimal;
 
@Component
@RequiredArgsConstructor
public class KakaoMobilityClient {
 
    private final WebClient kakaoMobilityWebClient;
 
    // 소요시간 계산 (분 단위 반환)
    public int getDurationMinutes(
            BigDecimal originLat, BigDecimal originLng,
            BigDecimal destLat, BigDecimal destLng,
            String transportType) {
 
        return switch (transportType) {
            case "CAR" -> getCarDuration(originLat, originLng, destLat, destLng);
            case "PUBLIC" -> getTransitDuration(originLat, originLng, destLat, destLng);
            case "WALK" -> getWalkDuration(originLat, originLng, destLat, destLng);
            default -> throw new IllegalArgumentException("지원하지 않는 이동수단입니다: " + transportType);
        };
    }
 
    // 자동차 소요시간
    private int getCarDuration(BigDecimal originLat, BigDecimal originLng,
                                BigDecimal destLat, BigDecimal destLng) {
        String response = kakaoMobilityWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/directions")
                        .queryParam("origin", originLng + "," + originLat)
                        .queryParam("destination", destLng + "," + destLat)
                        .queryParam("priority", "RECOMMEND")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
 
        return parseDurationSeconds(response) / 60;
    }
 
    // 대중교통 소요시간
    private int getTransitDuration(BigDecimal originLat, BigDecimal originLng,
                                    BigDecimal destLat, BigDecimal destLng) {
        String response = kakaoMobilityWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/directions/transit")
                        .queryParam("origin", originLng + "," + originLat)
                        .queryParam("destination", destLng + "," + destLat)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
 
        return parseDurationSeconds(response) / 60;
    }
 
    // 도보 소요시간
    private int getWalkDuration(BigDecimal originLat, BigDecimal originLng,
                                 BigDecimal destLat, BigDecimal destLng) {
        String response = kakaoMobilityWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/directions")
                        .queryParam("origin", originLng + "," + originLat)
                        .queryParam("destination", destLng + "," + destLat)
                        .queryParam("mode", "WALK")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
 
        return parseDurationSeconds(response) / 60;
    }
 
    // 카카오 모빌리티 응답에서 소요시간(초) 파싱
    private int parseDurationSeconds(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response);
            return root
                    .path("routes")
                    .get(0)
                    .path("summary")
                    .path("duration")
                    .asInt();
        } catch (Exception e) {
            throw new RuntimeException("카카오 모빌리티 API 응답 파싱 실패: " + e.getMessage());
        }
    }
}
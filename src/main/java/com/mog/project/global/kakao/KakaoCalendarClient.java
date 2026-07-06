package com.mog.project.global.kakao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
 
@Component
@RequiredArgsConstructor
public class KakaoCalendarClient {
 
    private final WebClient kakaoCalendarWebClient;
    private final UserRepository userRepository;
 
    // 톡캘린더 일정 등록
    public String createEvent(User user, String title, LocalDate date, LocalTime time) {
        String accessToken = getValidAccessToken(user);
 
        String startAt = formatDateTime(date, time);
        String endAt = formatDateTime(date, time.plusHours(1)); // 기본 1시간 일정
 
        String requestBody = buildEventBody(title, startAt, endAt);
 
        String response = kakaoCalendarWebClient.post()
                .uri("/v2/api/calendar/create/event")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
 
        return parseEventId(response);
    }
 
    // 톡캘린더 일정 수정
    public void updateEvent(User user, String eventId, String title, LocalDate date, LocalTime time) {
        String accessToken = getValidAccessToken(user);
 
        String startAt = formatDateTime(date, time);
        String endAt = formatDateTime(date, time.plusHours(1));
 
        String requestBody = buildEventBody(title, startAt, endAt);
 
        kakaoCalendarWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/api/calendar/update/event")
                        .queryParam("event_id", eventId)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
 
    // 톡캘린더 일정 삭제
    public void deleteEvent(User user, String eventId) {
        String accessToken = getValidAccessToken(user);
 
        kakaoCalendarWebClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/api/calendar/delete/event")
                        .queryParam("event_id", eventId)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
 
    // 액세스 토큰 유효성 확인 후 만료 시 재발급
    private String getValidAccessToken(User user) {
        if (user.getKakaoAccessToken() == null) {
            throw new IllegalStateException("카카오 액세스 토큰이 없습니다. 카카오 로그인이 필요합니다.");
        }
 
        // 토큰 만료 시 재발급
        if (user.getKakaoTokenExpiresAt() != null &&
                LocalDateTime.now().isAfter(user.getKakaoTokenExpiresAt().minusMinutes(5))) {
            return refreshAccessToken(user);
        }
 
        return user.getKakaoAccessToken();
    }
 
    // 카카오 액세스 토큰 재발급
    private String refreshAccessToken(User user) {
        if (user.getKakaoRefreshToken() == null) {
            throw new IllegalStateException("카카오 리프레시 토큰이 없습니다. 재로그인이 필요합니다.");
        }
 
        String response = kakaoCalendarWebClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .bodyValue("grant_type=refresh_token&refresh_token=" + user.getKakaoRefreshToken())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .retrieve()
                .bodyToMono(String.class)
                .block();
 
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response);
            String newAccessToken = root.path("access_token").asText();
            Integer expiresIn = root.path("expires_in").asInt();
 
            // DB에 새 액세스 토큰 업데이트
            user.updateKakaoToken(newAccessToken, null, LocalDateTime.now().plusSeconds(expiresIn));
            userRepository.save(user);
 
            return newAccessToken;
        } catch (Exception e) {
            throw new RuntimeException("카카오 액세스 토큰 재발급 실패: " + e.getMessage());
        }
    }
 
    // 일정 요청 바디 생성
    private String buildEventBody(String title, String startAt, String endAt) {
        return String.format(
                "{\"title\":\"%s\",\"time\":{\"start_at\":\"%s\",\"end_at\":\"%s\",\"time_zone\":\"Asia/Seoul\"}}",
                title, startAt, endAt
        );
    }
 
    // 날짜/시간 포맷 변환 (RFC3339)
    private String formatDateTime(LocalDate date, LocalTime time) {
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + "+09:00";
    }
 
    // 응답에서 event_id 파싱
    private String parseEventId(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response);
            return root.path("event_id").asText();
        } catch (Exception e) {
            throw new RuntimeException("카카오 톡캘린더 응답 파싱 실패: " + e.getMessage());
        }
    }
}
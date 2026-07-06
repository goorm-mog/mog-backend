package com.mog.project.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
 
@Configuration
public class WebClientConfig {
 
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoRestApiKey;
 
    // 카카오 기본 API (사용자 정보 등)
    @Bean
    public WebClient kakaoWebClient() {
        return WebClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
 
    // 카카오 모빌리티 API (소요시간 계산)
    @Bean
    public WebClient kakaoMobilityWebClient() {
        return WebClient.builder()
                .baseUrl("https://apis-navi.kakaomobility.com")
                .defaultHeader("Authorization", "KakaoAK " + kakaoRestApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
 
    // 카카오 톡캘린더 API (일정 등록)
    @Bean
    public WebClient kakaoCalendarWebClient() {
        return WebClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
package com.mog.project.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 채팅용 sub 추가
        config.enableSimpleBroker("/topic", "/queue", "/sub");

        // 채팅용 pub 추가
        config.setApplicationDestinationPrefixes("/app", "/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 프론트 주소로 변경 필수
                .withSockJS();

        // 채팅 스펙 전용 엔드포인트 추가 : ws://mo-ge.site/ws-stomp
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*"); // 순수 ws:// 프로토콜, SockJS X
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 두 엔드포인트(/ws, /ws-stomp) 모두 이 인터셉터 거침
        // CONNECT 프레임에서 JWT 검증
        registration.interceptors(stompAuthInterceptor);
    }
}
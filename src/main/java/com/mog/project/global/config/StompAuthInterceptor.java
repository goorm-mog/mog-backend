package com.mog.project.global.config;

import com.mog.project.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider; // 기존 Rest 인증에서 쓰던 것 재사용

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 클라이언트 -> 서버로 오는 모든 STOMP 프레임이 채널에 들어가기 전 이 메서드를 거쳐감
        // wrap()은 메시지와 분리된 accessor를 만들어 setUser()가 세션에 반영되지 않으므로,
        // 메시지에 실제로 연결된 accessor를 가져와야 함
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        //CONNECT 프레임일 때만 인증 검사 (SUBSCRIBE, SEND는 건너뜀 - 이미 CONNECT 때 인증된 세션이라 재검사 불필요)
        if(StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);

            if (token == null || !jwtProvider.isValid(token)) {
                throw new MessagingException("웹소켓 인증 실패 : 유효하지 않은 토큰입니다.");
            }

            String kakaoId = jwtProvider.getUserId(token); // 토큰 subject(카카오ID) 반환
            accessor.setUser(() -> kakaoId);
        }
        
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String bearer = accessor.getFirstNativeHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}

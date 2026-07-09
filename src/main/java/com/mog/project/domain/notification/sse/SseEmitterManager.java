package com.mog.project.domain.notification.sse;

import com.mog.project.global.redis.RedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterManager {

    // 현재 SSE 연결 중인 유저를 저장
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // RedisPublisher를 이용
    private final RedisPublisher redisPublisher;

    // 채널 이름
    private static final String CHANNEL_PREFIX = "notification:";

    // 유저가 /subscribe 엔드포인트에 접속하면 호출
    // SseEmitter를 생성해서 맵에 저장하고 클라이언트에 반환
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitters.put(userId, emitter);

        // 연결이 정상 종료되면 제거
        emitter.onCompletion(() -> emitters.remove(userId));

        // 타임아웃 발생 시 제거
        emitter.onTimeout(() -> emitters.remove(userId));

        // 에러 발생 시 제거
        emitter.onError(e -> emitters.remove(userId));

        try {
            // 연결 직후 "connect" 이벤트 발송: 성공 확인 여부용
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("{\"message\": \"SSE 연결이 수립되었습니다.\"}"));
        } catch (IOException e) {
            emitters.remove(userId);
        }
        return emitter;
    }

    // 알림 발생 시 호출: Redis 채널에 메시지를 발행
    public void publish(Long userId, String payload) {
        redisPublisher.publish(CHANNEL_PREFIX + userId, payload);
    }

    // Redis Subscriber 메시지를 받으면 메서드를 호출
    // 로컬 emitter 맵에서 해당 유저의 emitter를 꺼내 SSE 이벤트 발송
    public void sendToUser(Long userId, String payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(payload));
        } catch (IOException e) {
            emitters.remove(userId);
        }
    }

    // 모든 유저에게 heartbeat 전송
    public void sendHeartbeat() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("{}"));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        });
    }

}

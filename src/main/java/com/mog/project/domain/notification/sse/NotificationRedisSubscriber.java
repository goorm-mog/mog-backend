package com.mog.project.domain.notification.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber implements MessageListener {

    private final SseEmitterManager sseEmitterManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        Long userId = Long.parseLong(channel.split(":")[1]);
        String payload = new String(message.getBody());

        sseEmitterManager.sendToUser(userId, payload);
    }
}

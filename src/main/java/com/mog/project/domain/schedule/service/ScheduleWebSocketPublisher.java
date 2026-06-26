package com.mog.project.domain.schedule.service;

import com.mog.project.domain.schedule.dto.ConfirmedScheduleResponse;
import com.mog.project.domain.schedule.dto.SlotListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
 
@Component
@RequiredArgsConstructor
public class ScheduleWebSocketPublisher {
 
    private final SimpMessagingTemplate messagingTemplate;
 
    // 투표 현황 브로드캐스트 (투표 등록/취소 시 호출)
    public void publishVoteUpdate(Long roomId, SlotListResponse payload) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/schedule/votes", payload
        );
    }
 
    // 일정 확정 브로드캐스트 (방장이 확정 시 호출)
    public void publishConfirm(Long roomId, ConfirmedScheduleResponse payload) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/schedule/confirm", payload
        );
    }
}
 
package com.mog.project.domain.midpoint.service;

import com.mog.project.domain.midpoint.dto.ConfirmedPlaceResponse;
import com.mog.project.domain.midpoint.dto.DepartureLocationListResponse;
import com.mog.project.domain.midpoint.dto.MiddlePointResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
 
@Component
@RequiredArgsConstructor
public class MidpointWebSocketPublisher {
 
    private final SimpMessagingTemplate messagingTemplate;
 
    // 출발지 등록/수정 시 브로드캐스트
    public void publishDepartureUpdate(Long roomId, DepartureLocationListResponse payload) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/departure", payload
        );
    }
 
    // 중간지점 + 소요시간 계산 완료 시 브로드캐스트
    public void publishMiddlePoint(Long roomId, MiddlePointResponse payload) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/midpoint", payload
        );
    }
 
    // 장소 확정 시 브로드캐스트
    public void publishConfirmedPlace(Long roomId, ConfirmedPlaceResponse payload) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/midpoint/confirm", payload
        );
    }
}
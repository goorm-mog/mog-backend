package com.mog.project.domain.schedule.dto;

public record ScheduleStatusResponse(
        Long roomId,
        String status,       // WAITING | SCHEDULE_VOTING | DEPARTURE_INPUT | MIDPOINT_FINDING | COMPLETED
        String description   // 현재 상태 설명
) {
    public static ScheduleStatusResponse waiting(Long roomId) {
        return new ScheduleStatusResponse(roomId, "WAITING", "방장이 투표 슬롯을 아직 등록하지 않았습니다.");
    }
 
    public static ScheduleStatusResponse scheduleVoting(Long roomId) {
        return new ScheduleStatusResponse(roomId, "SCHEDULE_VOTING", "날짜 및 시간 투표 진행 중입니다.");
    }
 
    public static ScheduleStatusResponse departureInput(Long roomId) {
        return new ScheduleStatusResponse(roomId, "DEPARTURE_INPUT", "출발지 입력 진행 중입니다.");
    }
 
    public static ScheduleStatusResponse midpointFinding(Long roomId) {
        return new ScheduleStatusResponse(roomId, "MIDPOINT_FINDING", "중간지점 찾기 진행 중입니다.");
    }
 
    public static ScheduleStatusResponse completed(Long roomId) {
        return new ScheduleStatusResponse(roomId, "COMPLETED", "중간지점 계산이 완료되었습니다.");
    }
}
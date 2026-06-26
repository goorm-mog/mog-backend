package com.mog.project.domain.schedule.dto;

import com.mog.project.domain.schedule.entity.ConfirmedSchedule;
 
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
 
public record ConfirmedScheduleResponse(
        Long confirmedId,
        Long roomId,
        LocalDate date,
        LocalTime time,
        Long confirmedBy,
        String kakaoEventId,
        LocalDateTime confirmedAt
) {
    public static ConfirmedScheduleResponse from(ConfirmedSchedule confirmed) {
        return new ConfirmedScheduleResponse(
                confirmed.getId(),
                confirmed.getRoomId(),
                confirmed.getConfirmedDate(),
                confirmed.getConfirmedTime(),
                confirmed.getConfirmedBy(),
                confirmed.getKakaoEventId(),
                confirmed.getConfirmedAt()
        );
    }
}
 
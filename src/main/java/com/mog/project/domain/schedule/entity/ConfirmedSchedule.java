package com.mog.project.domain.schedule.entity;
 
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
 
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "confirmed_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfirmedSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "confirmed_schedule_id")
    private Long id;

    //room_id UNIQUE → 모임방당 확정 일정 최대 1건
    @Column(name = "room_id", nullable = false, unique = true)
    private Long roomId;

    @Column(name = "confirmed_date", nullable = false)
    private LocalDate confirmedDate;

    @Column(name = "confirmed_time", nullable = false)
    private LocalTime confirmedTime;

    @Column(name = "confirmed_by", nullable = false)
    private Long confirmedBy;  // 확정한 방장 userId

    //카카오 톡캘린더 API 등록 후 받는 이벤트 ID
    @Column(name = "kakao_event_id")
    private String kakaoEventId;


    @Column(name = "confirmed_at", nullable = false, updatable = false)
    private LocalDateTime confirmedAt;

    @Builder
    public ConfirmedSchedule(Long roomId, LocalDate confirmedDate, LocalTime confirmedTime, Long confirmedBy) {
        this.roomId = roomId;
        this.confirmedDate = confirmedDate;
        this.confirmedTime = confirmedTime;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = LocalDateTime.now();
    }
    // 톡캘린더 연동 후 이벤트 ID 업데이트용 (나중에 사용)
    public void updateKakaoEventId(String kakaoEventId) {
        this.kakaoEventId = kakaoEventId;
    }
 
    // 방장이 확정 일정을 수정할 때 사용
    public void update(LocalDate confirmedDate, LocalTime confirmedTime) {
        this.confirmedDate = confirmedDate;
        this.confirmedTime = confirmedTime;
    }
}
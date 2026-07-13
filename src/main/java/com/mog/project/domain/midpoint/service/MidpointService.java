package com.mog.project.domain.midpoint.service;

import com.mog.project.domain.midpoint.dto.ConfirmPlaceRequest;
import com.mog.project.domain.midpoint.dto.ConfirmedPlaceResponse;
import com.mog.project.domain.midpoint.dto.DepartureLocationListResponse;
import com.mog.project.domain.midpoint.dto.DepartureLocationRequest;
import com.mog.project.domain.midpoint.dto.DepartureLocationResponse;
import com.mog.project.domain.midpoint.dto.MiddlePointResponse;
import com.mog.project.domain.midpoint.entity.ConfirmedPlace;
import com.mog.project.domain.midpoint.entity.DepartureLocation;
import com.mog.project.domain.midpoint.entity.MiddlePoint;
import com.mog.project.domain.midpoint.entity.TravelTime;
import com.mog.project.domain.groups.repository.GroupMemberRepository;
import com.mog.project.domain.midpoint.repository.ConfirmedPlaceRepository;
import com.mog.project.domain.midpoint.repository.DepartureLocationRepository;
import com.mog.project.domain.midpoint.repository.MiddlePointRepository;
import com.mog.project.domain.midpoint.repository.TravelTimeRepository;
import com.mog.project.domain.notification.entity.NotificationType;
import com.mog.project.domain.notification.service.NotificationService;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.repository.RoomRepository;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import com.mog.project.global.kakao.KakaoMobilityClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
 
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MidpointService {
 
    private static final int MAX_ITERATIONS = 3;
    private static final double MOVE_RATIO = 0.3;
 
    private final DepartureLocationRepository departureLocationRepository;
    private final MiddlePointRepository middlePointRepository;
    private final ConfirmedPlaceRepository confirmedPlaceRepository;
    private final TravelTimeRepository travelTimeRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final KakaoMobilityClient kakaoMobilityClient;
    private final MidpointWebSocketPublisher midpointWebSocketPublisher;
    private final NotificationService notificationService;
 
    // kakaoId → User 변환 공통 메서드
    private User getUser(String kakaoId) {
        return userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new GlobalException(ErrorCode.UNAUTHORIZED_USER));
    }
 
    // 방장 권한 체크 공통 메서드
    private void validateCreator(Room room, Long userId) {
        if (!Objects.equals(room.getCreator().getUserId(), userId)) {
            throw new GlobalException(ErrorCode.NOT_ROOM_LEADER);
        }
    }
 
    // Room 조회 공통 메서드
    private Room getRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROOM_NOT_FOUND));
    }
 
    // ──────────────────────────────────────────
    // 1. 출발지 등록
    // ──────────────────────────────────────────
    @Transactional
    public DepartureLocationResponse registerDeparture(Long roomId, String kakaoId, DepartureLocationRequest request) {
        User user = getUser(kakaoId);
 
        if (departureLocationRepository.existsByRoomIdAndUserId(roomId, user.getUserId())) {
            throw new IllegalStateException("이미 출발지가 등록되어 있습니다. 수정은 PATCH를 사용해주세요.");
        }
 
        DepartureLocation departure = DepartureLocation.builder()
                .roomId(roomId)
                .userId(user.getUserId())
                .placeName(request.placeName())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .transportType(request.transportType())
                .build();
 
        DepartureLocationResponse response = DepartureLocationResponse.from(departureLocationRepository.save(departure));
 
        // 출발지 목록 WS 브로드캐스트
        midpointWebSocketPublisher.publishDepartureUpdate(roomId, getDepartures(roomId));
 
        return response;
    }
 
    // ──────────────────────────────────────────
    // 2. 출발지 수정
    // ──────────────────────────────────────────
    @Transactional
    public DepartureLocationResponse updateDeparture(Long roomId, String kakaoId, DepartureLocationRequest request) {
        User user = getUser(kakaoId);
 
        DepartureLocation departure = departureLocationRepository.findByRoomIdAndUserId(roomId, user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("등록된 출발지가 없습니다. 먼저 POST로 등록해주세요."));
 
        departure.update(
                request.placeName(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.transportType()
        );
 
        DepartureLocationResponse response = DepartureLocationResponse.from(departure);
 
        // 출발지 목록 WS 브로드캐스트
        midpointWebSocketPublisher.publishDepartureUpdate(roomId, getDepartures(roomId));
 
        return response;
    }
 
    // ──────────────────────────────────────────
    // 3. 출발지 목록 조회 (address 포함)
    // ──────────────────────────────────────────
    public DepartureLocationListResponse getDepartures(Long roomId) {
        List<DepartureLocation> departures = departureLocationRepository.findAllByRoomId(roomId);
        return DepartureLocationListResponse.from(roomId, departures);
    }
 
    // ──────────────────────────────────────────
    // 4. 중간지점 계산 + 소요시간 계산 (방장만 가능)
    // 이진 탐색 방식으로 카카오 모빌리티 API 활용
    // ──────────────────────────────────────────
    @Transactional
    public MiddlePointResponse calculateMiddlePoint(Long roomId, String kakaoId) {
        User user = getUser(kakaoId);
        Room room = getRoom(roomId);
        validateCreator(room, user.getUserId());
 
        List<DepartureLocation> departures = departureLocationRepository.findAllByRoomId(roomId);
 
        if (departures.isEmpty()) {
            throw new IllegalStateException("등록된 출발지가 없습니다.");
        }
 
        // 1단계: 위도/경도 평균으로 초기 후보 지점 설정
        BigDecimal candidateLat = departures.stream()
                .map(DepartureLocation::getLatitude)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(departures.size()), 7, RoundingMode.HALF_UP);
 
        BigDecimal candidateLng = departures.stream()
                .map(DepartureLocation::getLongitude)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(departures.size()), 7, RoundingMode.HALF_UP);
 
        // 2~4단계: 이진 탐색으로 후보 지점 이동 (3회 반복)
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            int maxDuration = -1;
            DepartureLocation farthest = null;
 
            for (DepartureLocation departure : departures) {
                int duration = kakaoMobilityClient.getDurationMinutes(
                        departure.getLatitude(),
                        departure.getLongitude(),
                        candidateLat,
                        candidateLng,
                        departure.getTransportType()
                );
 
                if (duration > maxDuration) {
                    maxDuration = duration;
                    farthest = departure;
                }
            }
 
            if (farthest != null) {
                candidateLat = candidateLat.add(
                        farthest.getLatitude().subtract(candidateLat)
                                .multiply(BigDecimal.valueOf(MOVE_RATIO))
                ).setScale(7, RoundingMode.HALF_UP);
 
                candidateLng = candidateLng.add(
                        farthest.getLongitude().subtract(candidateLng)
                                .multiply(BigDecimal.valueOf(MOVE_RATIO))
                ).setScale(7, RoundingMode.HALF_UP);
            }
        }
 
        final BigDecimal finalLat = candidateLat;
        final BigDecimal finalLng = candidateLng;
 
        // 5단계: 중간지점 저장 (upsert)
        MiddlePoint middlePoint = middlePointRepository.findByRoomId(roomId)
                .map(existing -> {
                    existing.update(finalLat, finalLng);
                    return existing;
                })
                .orElseGet(() -> middlePointRepository.save(
                        MiddlePoint.builder()
                                .roomId(roomId)
                                .latitude(finalLat)
                                .longitude(finalLng)
                                .build()
                ));
 
        // 6단계: 소요시간 저장 (upsert)
        for (DepartureLocation departure : departures) {
            int durationMinutes = kakaoMobilityClient.getDurationMinutes(
                    departure.getLatitude(),
                    departure.getLongitude(),
                    finalLat,
                    finalLng,
                    departure.getTransportType()
            );
 
            travelTimeRepository.findByRoomIdAndUserId(roomId, departure.getUserId())
                    .ifPresentOrElse(
                            existing -> existing.update(durationMinutes),
                            () -> travelTimeRepository.save(
                                    TravelTime.builder()
                                            .roomId(roomId)
                                            .userId(departure.getUserId())
                                            .durationMinutes(durationMinutes)
                                            .transportType(departure.getTransportType())
                                            .build()
                            )
                    );
        }
 
        List<TravelTime> travelTimes = travelTimeRepository.findAllByRoomId(roomId);
        MiddlePointResponse response = MiddlePointResponse.from(middlePoint, travelTimes);
 
        // 중간지점 + 소요시간 WS 브로드캐스트
        midpointWebSocketPublisher.publishMiddlePoint(roomId, response);
 
        return response;
    }
 
    // ──────────────────────────────────────────
    // 5. 중간지점 조회
    // ──────────────────────────────────────────
    public MiddlePointResponse getMiddlePoint(Long roomId) {
        MiddlePoint middlePoint = middlePointRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("아직 중간지점이 계산되지 않았습니다."));
        List<TravelTime> travelTimes = travelTimeRepository.findAllByRoomId(roomId);
        return MiddlePointResponse.from(middlePoint, travelTimes);
    }
 
    // ──────────────────────────────────────────
    // 6. 장소 확정 (방장만 가능)
    // ──────────────────────────────────────────
    @Transactional
    public ConfirmedPlaceResponse confirmPlace(Long roomId, String kakaoId, ConfirmPlaceRequest request) {
        User user = getUser(kakaoId);
        Room room = getRoom(roomId);
        validateCreator(room, user.getUserId());
 
        ConfirmedPlace confirmedPlace = confirmedPlaceRepository.findByRoomId(roomId)
                .map(existing -> {
                    existing.update(
                            request.kakaoPlaceId(),
                            request.placeName(),
                            request.address(),
                            request.category(),
                            request.latitude(),
                            request.longitude()
                    );
                    return existing;
                })
                .orElseGet(() -> confirmedPlaceRepository.save(
                        ConfirmedPlace.builder()
                                .roomId(roomId)
                                .kakaoPlaceId(request.kakaoPlaceId())
                                .placeName(request.placeName())
                                .address(request.address())
                                .category(request.category())
                                .latitude(request.latitude())
                                .longitude(request.longitude())
                                .build()
                ));
 
        ConfirmedPlaceResponse response = ConfirmedPlaceResponse.from(confirmedPlace);

        // 장소 확정 WS 브로드캐스트
        midpointWebSocketPublisher.publishConfirmedPlace(roomId, response);

        String message = "[" + room.getRoomName() + "] 장소가 확정됐습니다.";
        groupMemberRepository.findByGroupGroupId(room.getGroup().getGroupId())
                .forEach(gm -> notificationService.send(
                        gm.getUser().getUserId(),
                        NotificationType.PLACE_CONFIRMED,
                        message,
                        roomId
                ));

        return response;
    }
}
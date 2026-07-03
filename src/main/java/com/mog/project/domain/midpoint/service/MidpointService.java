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
import com.mog.project.domain.midpoint.repository.ConfirmedPlaceRepository;
import com.mog.project.domain.midpoint.repository.DepartureLocationRepository;
import com.mog.project.domain.midpoint.repository.MiddlePointRepository;
import com.mog.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
 
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MidpointService {
 
    private final DepartureLocationRepository departureLocationRepository;
    private final MiddlePointRepository middlePointRepository;
    private final ConfirmedPlaceRepository confirmedPlaceRepository;
    private final UserRepository userRepository;
 
    // kakaoId → userId 변환 공통 메서드
    private Long getUserId(String kakaoId) {
        return userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."))
                .getUserId();
    }
 
    // ──────────────────────────────────────────
    // 1. 출발지 등록
    // ──────────────────────────────────────────
    @Transactional
    public DepartureLocationResponse registerDeparture(Long roomId, String kakaoId, DepartureLocationRequest request) {
        Long userId = getUserId(kakaoId);
 
        // 이미 등록된 출발지가 있으면 예외
        if (departureLocationRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new IllegalStateException("이미 출발지가 등록되어 있습니다. 수정은 PATCH를 사용해주세요.");
        }
 
        DepartureLocation departure = DepartureLocation.builder()
                .roomId(roomId)
                .userId(userId)
                .placeName(request.placeName())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .transportType(request.transportType())
                .build();
 
        return DepartureLocationResponse.from(departureLocationRepository.save(departure));
    }
 
    // ──────────────────────────────────────────
    // 2. 출발지 수정
    // ──────────────────────────────────────────
    @Transactional
    public DepartureLocationResponse updateDeparture(Long roomId, String kakaoId, DepartureLocationRequest request) {
        Long userId = getUserId(kakaoId);
 
        DepartureLocation departure = departureLocationRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("등록된 출발지가 없습니다. 먼저 POST로 등록해주세요."));
 
        departure.update(
                request.placeName(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.transportType()
        );
 
        return DepartureLocationResponse.from(departure);
    }
 
    // ──────────────────────────────────────────
    // 3. 출발지 목록 조회 (address 포함)
    // ──────────────────────────────────────────
    public DepartureLocationListResponse getDepartures(Long roomId) {
        List<DepartureLocation> departures = departureLocationRepository.findAllByRoomId(roomId);
        return DepartureLocationListResponse.from(roomId, departures);
    }
 
    // ──────────────────────────────────────────
    // 4. 중간지점 계산
    // ──────────────────────────────────────────
    @Transactional
    public MiddlePointResponse calculateMiddlePoint(Long roomId) {
        List<DepartureLocation> departures = departureLocationRepository.findAllByRoomId(roomId);
 
        if (departures.isEmpty()) {
            throw new IllegalStateException("등록된 출발지가 없습니다.");
        }
 
        // 위도/경도 평균으로 중간지점 계산
        BigDecimal avgLatitude = departures.stream()
                .map(DepartureLocation::getLatitude)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(departures.size()), 7, RoundingMode.HALF_UP);
 
        BigDecimal avgLongitude = departures.stream()
                .map(DepartureLocation::getLongitude)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(departures.size()), 7, RoundingMode.HALF_UP);
 
        // upsert - 이미 계산된 경우 덮어쓰기
        MiddlePoint middlePoint = middlePointRepository.findByRoomId(roomId)
                .map(existing -> {
                    existing.update(avgLatitude, avgLongitude);
                    return existing;
                })
                .orElseGet(() -> middlePointRepository.save(
                        MiddlePoint.builder()
                                .roomId(roomId)
                                .latitude(avgLatitude)
                                .longitude(avgLongitude)
                                .build()
                ));
 
        return MiddlePointResponse.from(middlePoint);
    }
 
    // ──────────────────────────────────────────
    // 5. 중간지점 조회
    // ──────────────────────────────────────────
    public MiddlePointResponse getMiddlePoint(Long roomId) {
        MiddlePoint middlePoint = middlePointRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("아직 중간지점이 계산되지 않았습니다."));
        return MiddlePointResponse.from(middlePoint);
    }
 
    // ──────────────────────────────────────────
    // 6. 장소 확정 (방장)
    // ──────────────────────────────────────────
    @Transactional
    public ConfirmedPlaceResponse confirmPlace(Long roomId, String kakaoId, ConfirmPlaceRequest request) {
        Long userId = getUserId(kakaoId);
 
        // upsert - 이미 확정된 경우 덮어쓰기
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
 
        return ConfirmedPlaceResponse.from(confirmedPlace);
    }
}
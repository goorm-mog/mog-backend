package com.mog.project.domain.midpoint.service;

import com.mog.project.domain.midpoint.dto.ConfirmPlaceRequest;
import com.mog.project.domain.midpoint.dto.ConfirmedPlaceResponse;
import com.mog.project.domain.midpoint.dto.DepartureLocationListResponse;
import com.mog.project.domain.midpoint.dto.DepartureLocationRequest;
import com.mog.project.domain.midpoint.dto.DepartureLocationResponse;
import com.mog.project.domain.midpoint.dto.MiddlePointResponse;
import com.mog.project.domain.midpoint.dto.TravelTimeResponse;
import com.mog.project.domain.midpoint.entity.ConfirmedPlace;
import com.mog.project.domain.midpoint.entity.DepartureLocation;
import com.mog.project.domain.midpoint.entity.MiddlePoint;
import com.mog.project.domain.midpoint.entity.TravelTime;
import com.mog.project.domain.midpoint.repository.ConfirmedPlaceRepository;
import com.mog.project.domain.midpoint.repository.DepartureLocationRepository;
import com.mog.project.domain.midpoint.repository.MiddlePointRepository;
import com.mog.project.domain.midpoint.repository.TravelTimeRepository;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.repository.RoomRepository;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.exception.GlobalException;
import com.mog.project.global.kakao.KakaoMobilityClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
 
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
class MidpointServiceTest {
 
    @Mock private DepartureLocationRepository departureLocationRepository;
    @Mock private MiddlePointRepository middlePointRepository;
    @Mock private ConfirmedPlaceRepository confirmedPlaceRepository;
    @Mock private TravelTimeRepository travelTimeRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private KakaoMobilityClient kakaoMobilityClient;
 
    @InjectMocks
    private MidpointService midpointService;
 
    private final String kakaoId = "kakao_123";
    private final Long userId = 10L;
    private final Long roomId = 1L;
 
    // 공통 Mock 설정
    private User mockUser() {
        User user = User.builder()
                .kakaoId(kakaoId)
                .nickname("테스트유저")
                .email("test@test.com")
                .profileImageUrl(null)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }
 
    private Room mockRoom(Long creatorId) {
        User creator = User.builder()
                .kakaoId(kakaoId)
                .nickname("방장")
                .email("test@test.com")
                .profileImageUrl(null)
                .build();
        ReflectionTestUtils.setField(creator, "userId", creatorId);
 
        Room room = mock(Room.class);
        given(room.getCreator()).willReturn(creator);
        given(room.getDeletedAt()).willReturn(null);
        return room;
    }
 
    private DepartureLocation mockDeparture(Long userId) {
        DepartureLocation departure = DepartureLocation.builder()
                .roomId(roomId)
                .userId(userId)
                .placeName("강남역")
                .address("서울 강남구 강남대로 396")
                .latitude(new BigDecimal("37.4979000"))
                .longitude(new BigDecimal("127.0276000"))
                .transportType("PUBLIC")
                .build();
        ReflectionTestUtils.setField(departure, "id", 1L);
        return departure;
    }
 
    // ──────────────────────────────────────────
    // 1. 출발지 등록
    // ──────────────────────────────────────────
    @Test
    @DisplayName("출발지 등록 성공")
    void registerDeparture_success() {
        // given
        User user = mockUser();
        given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(user));
        given(departureLocationRepository.existsByRoomIdAndUserId(roomId, userId)).willReturn(false);
        given(departureLocationRepository.save(any())).willReturn(mockDeparture(userId));
 
        DepartureLocationRequest request = new DepartureLocationRequest(
                "강남역", "서울 강남구 강남대로 396",
                new BigDecimal("37.4979000"), new BigDecimal("127.0276000"), "PUBLIC"
        );
 
        // when
        DepartureLocationResponse response = midpointService.registerDeparture(roomId, kakaoId, request);
 
        // then
        assertThat(response.placeName()).isEqualTo("강남역");
        verify(departureLocationRepository, times(1)).save(any());
    }
 
    @Test
    @DisplayName("출발지 등록 실패 - 이미 등록된 출발지")
    void registerDeparture_fail_alreadyExists() {
        // given
        User user = mockUser();
        given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(user));
        given(departureLocationRepository.existsByRoomIdAndUserId(roomId, userId)).willReturn(true);
 
        DepartureLocationRequest request = new DepartureLocationRequest(
                "강남역", "서울 강남구 강남대로 396",
                new BigDecimal("37.4979000"), new BigDecimal("127.0276000"), "PUBLIC"
        );
 
        // when & then
        assertThatThrownBy(() -> midpointService.registerDeparture(roomId, kakaoId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 출발지가 등록되어 있습니다. 수정은 PATCH를 사용해주세요.");
    }
 
    // ──────────────────────────────────────────
    // 2. 출발지 수정
    // ──────────────────────────────────────────
    @Test
    @DisplayName("출발지 수정 성공")
    void updateDeparture_success() {
        // given
        User user = mockUser();
        DepartureLocation departure = mockDeparture(userId);
        given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(user));
        given(departureLocationRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(departure));
 
        DepartureLocationRequest request = new DepartureLocationRequest(
                "선릉역", "서울 강남구 테헤란로 211",
                new BigDecimal("37.5047000"), new BigDecimal("127.0498000"), "CAR"
        );
 
        // when
        DepartureLocationResponse response = midpointService.updateDeparture(roomId, kakaoId, request);
 
        // then
        assertThat(response.placeName()).isEqualTo("선릉역");
        assertThat(response.transportType()).isEqualTo("CAR");
    }
 
    @Test
    @DisplayName("출발지 수정 실패 - 등록된 출발지 없음")
    void updateDeparture_fail_notFound() {
        // given
        User user = mockUser();
        given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(user));
        given(departureLocationRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.empty());
 
        DepartureLocationRequest request = new DepartureLocationRequest(
                "선릉역", "서울 강남구 테헤란로 211",
                new BigDecimal("37.5047000"), new BigDecimal("127.0498000"), "CAR"
        );
 
        // when & then
        assertThatThrownBy(() -> midpointService.updateDeparture(roomId, kakaoId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("등록된 출발지가 없습니다. 먼저 POST로 등록해주세요.");
    }
 
    // ──────────────────────────────────────────
    // 3. 출발지 목록 조회
    // ──────────────────────────────────────────
    @Test
    @DisplayName("출발지 목록 조회 성공")
    void getDepartures_success() {
        // given
        given(departureLocationRepository.findAllByRoomId(roomId)).willReturn(List.of(mockDeparture(userId)));
 
        // when
        DepartureLocationListResponse response = midpointService.getDepartures(roomId);
 
        // then
        assertThat(response.departures()).hasSize(1);
        assertThat(response.submittedCount()).isEqualTo(1);
    }
 
    // ──────────────────────────────────────────
    // 4. 중간지점 계산
    // ──────────────────────────────────────────
    @Test
    @DisplayName("중간지점 계산 성공 - 처음 계산")
    void calculateMiddlePoint_success_new() {
        // given
        User user = mockUser();
        Room room = mockRoom(userId);
        given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(user));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(departureLocationRepository.findAllByRoomId(roomId)).willReturn(List.of(mockDeparture(userId)));
 
        MiddlePoint middlePoint = MiddlePoint.builder()
                .roomId(roomId)
                .latitude(new BigDecimal("37.4979000"))
                .longitude(new BigDecimal("127.0276000"))
                .build();
 
        given(middlePointRepository.findByRoomId(roomId)).willReturn(Optional.empty());
        given(middlePointRepository.save(any())).willReturn(middlePoint);
 
        // when
        MiddlePointResponse response = midpointService.calculateMiddlePoint(roomId, kakaoId);
 
        // then
        assertThat(response.latitude()).isEqualTo(new BigDecimal("37.4979000"));
        verify(middlePointRepository, times(1)).save(any());
    }
 
    @Test
    @DisplayName("중간지점 계산 실패 - 방장 아님")
    void calculateMiddlePoint_fail_notCreator() {
        // given
        User user = mockUser();
        Room room = mockRoom(999L); // 다른 유저가 방장
        given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(user));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
 
        // when & then
        assertThatThrownBy(() -> midpointService.calculateMiddlePoint(roomId, kakaoId))
                .isInstanceOf(GlobalException.class);
    }
 
    // ──────────────────────────────────────────
    // 5. 중간지점 조회
    // ──────────────────────────────────────────
    @Test
    @DisplayName("중간지점 조회 성공")
    void getMiddlePoint_success() {
        // given
        MiddlePoint middlePoint = MiddlePoint.builder()
                .roomId(roomId)
                .latitude(new BigDecimal("37.4979000"))
                .longitude(new BigDecimal("127.0276000"))
                .build();
        given(middlePointRepository.findByRoomId(roomId)).willReturn(Optional.of(middlePoint));
 
        // when
        MiddlePointResponse response = midpointService.getMiddlePoint(roomId);
 
        // then
        assertThat(response.latitude()).isEqualTo(new BigDecimal("37.4979000"));
    }
 
    @Test
    @DisplayName("중간지점 조회 실패 - 아직 계산 전")
    void getMiddlePoint_fail_notCalculated() {
        // given
        given(middlePointRepository.findByRoomId(roomId)).willReturn(Optional.empty());
 
        // when & then
        assertThatThrownBy(() -> midpointService.getMiddlePoint(roomId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아직 중간지점이 계산되지 않았습니다.");
    }
 
    // ──────────────────────────────────────────
    // 6. 장소 확정
    // ──────────────────────────────────────────
    @Test
    @DisplayName("장소 확정 성공 - 처음 확정")
    void confirmPlace_success_new() {
        // given
        User user = mockUser();
        Room room = mockRoom(userId);
        given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(user));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(confirmedPlaceRepository.findByRoomId(roomId)).willReturn(Optional.empty());
 
        ConfirmPlaceRequest request = new ConfirmPlaceRequest(
                "12345678", "스시조", "서울 강남구 ...", "FD6",
                new BigDecimal("37.5015000"), new BigDecimal("127.0390000")
        );
 
        ConfirmedPlace confirmedPlace = ConfirmedPlace.builder()
                .roomId(roomId)
                .kakaoPlaceId(request.kakaoPlaceId())
                .placeName(request.placeName())
                .address(request.address())
                .category(request.category())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();
 
        given(confirmedPlaceRepository.save(any())).willReturn(confirmedPlace);
 
        // when
        ConfirmedPlaceResponse response = midpointService.confirmPlace(roomId, kakaoId, request);
 
        // then
        assertThat(response.placeName()).isEqualTo("스시조");
        verify(confirmedPlaceRepository, times(1)).save(any());
    }
 
    // ──────────────────────────────────────────
    // 7. 인원별 소요시간 계산
    // ──────────────────────────────────────────
    @Test
    @DisplayName("소요시간 계산 성공")
    void calculateTravelTimes_success() {
        // given
        User user = mockUser();
        Room room = mockRoom(userId);
        given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(user));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
 
        MiddlePoint middlePoint = MiddlePoint.builder()
                .roomId(roomId)
                .latitude(new BigDecimal("37.5012000"))
                .longitude(new BigDecimal("127.0386000"))
                .build();
        given(middlePointRepository.findByRoomId(roomId)).willReturn(Optional.of(middlePoint));
        given(departureLocationRepository.findAllByRoomId(roomId)).willReturn(List.of(mockDeparture(userId)));
        given(kakaoMobilityClient.getDurationMinutes(any(), any(), any(), any(), any())).willReturn(23);
        given(travelTimeRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.empty());
 
        TravelTime travelTime = TravelTime.builder()
                .roomId(roomId)
                .userId(userId)
                .durationMinutes(23)
                .transportType("PUBLIC")
                .build();
        given(travelTimeRepository.findAllByRoomId(roomId)).willReturn(List.of(travelTime));
 
        // when
        TravelTimeResponse response = midpointService.calculateTravelTimes(roomId, kakaoId);
 
        // then
        assertThat(response.travelTimes()).hasSize(1);
        assertThat(response.travelTimes().get(0).durationMinutes()).isEqualTo(23);
        verify(travelTimeRepository, times(1)).save(any());
    }
 
    @Test
    @DisplayName("소요시간 계산 실패 - 중간지점 미계산")
    void calculateTravelTimes_fail_noMiddlePoint() {
        // given
        User user = mockUser();
        Room room = mockRoom(userId);
        given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(user));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(middlePointRepository.findByRoomId(roomId)).willReturn(Optional.empty());
 
        // when & then
        assertThatThrownBy(() -> midpointService.calculateTravelTimes(roomId, kakaoId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("중간지점이 먼저 계산되어야 합니다.");
    }
}
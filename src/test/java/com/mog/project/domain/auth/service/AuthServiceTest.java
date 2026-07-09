package com.mog.project.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mog.project.domain.auth.dto.request.KakaoLoginRequest;
import com.mog.project.domain.auth.dto.response.AccessTokenReissueResponse;
import com.mog.project.domain.auth.dto.response.AuthLoginResponse;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.auth.jwt.JwtProvider;
import com.mog.project.global.auth.oauth2.client.KakaoApiClient;
import com.mog.project.global.auth.oauth2.client.KakaoOAuthClient;
import com.mog.project.global.auth.oauth2.dto.KakaoUserInfoResponse;
import com.mog.project.global.exception.AuthException;
import com.mog.project.global.exception.ErrorCode;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-characters-long";
    private static final long ACCESS_EXPIRY = 1800000L;
    private static final long REFRESH_EXPIRY = 604800000L;

    @Mock private KakaoApiClient kakaoApiClient;
    @Mock private KakaoOAuthClient kakaoOAuthClient;
    @Mock private UserRepository userRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private JwtProvider jwtProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
        authService = new AuthService(kakaoApiClient, kakaoOAuthClient, userRepository, jwtProvider, redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ---- 카카오 로그인 ----

    @Test
    void 신규_유저_로그인시_DB에_저장하고_토큰을_반환한다() {
        KakaoUserInfoResponse kakaoInfo = kakaoUserInfo(999L, "신규유저", "new@kakao.com", "http://img.url");
        given(kakaoApiClient.getUserInfo("kakao-at")).willReturn(kakaoInfo);
        given(userRepository.findByKakaoId("999")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        AuthLoginResponse response = authService.kakaoLogin(new KakaoLoginRequest("kakao-at"), new MockHttpServletResponse());

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().nickname()).isEqualTo("신규유저");
        verify(userRepository).save(any(User.class));
        verify(valueOperations).set(eq("RT:999"), anyString(), eq(REFRESH_EXPIRY), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void 기존_유저_로그인시_프로필이_업데이트되고_토큰을_반환한다() {
        User existing = user("999", "옛날닉네임", "http://old.url");
        KakaoUserInfoResponse kakaoInfo = kakaoUserInfo(999L, "새닉네임", "test@kakao.com", "http://new.url");
        given(kakaoApiClient.getUserInfo("kakao-at")).willReturn(kakaoInfo);
        given(userRepository.findByKakaoId("999")).willReturn(Optional.of(existing));

        authService.kakaoLogin(new KakaoLoginRequest("kakao-at"), new MockHttpServletResponse());

        assertThat(existing.getNickname()).isEqualTo("새닉네임");
        assertThat(existing.getProfileImageUrl()).isEqualTo("http://new.url");
        verify(userRepository, never()).save(any());
    }

    @Test
    void 로그인_응답에_HttpOnly_쿠키가_설정된다() {
        KakaoUserInfoResponse kakaoInfo = kakaoUserInfo(1L, "유저", "u@kakao.com", null);
        given(kakaoApiClient.getUserInfo("kakao-at")).willReturn(kakaoInfo);
        given(userRepository.findByKakaoId("1")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        authService.kakaoLogin(new KakaoLoginRequest("kakao-at"), httpResponse);

        String setCookie = httpResponse.getHeader("Set-Cookie");
        assertThat(setCookie).contains("refreshToken=");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Path=/api/v1/auth");
        assertThat(setCookie).contains("SameSite=Strict");
    }

    // ---- 토큰 재발급 (RTR) ----

    @Test
    void 유효한_RT로_재발급시_새_AT와_RT를_반환한다() {
        JwtProvider shortExpiry = new JwtProvider(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
        String rt = shortExpiry.generateRefreshToken("42");
        given(valueOperations.get("RT:42")).willReturn(rt);
        given(userRepository.findByKakaoId("42")).willReturn(Optional.of(user("42", "테스트유저", null)));

        AuthService svc = new AuthService(kakaoApiClient, kakaoOAuthClient, userRepository, shortExpiry, redisTemplate);
        AccessTokenReissueResponse result = svc.reissue(rt, new MockHttpServletResponse());

        assertThat(result.accessToken()).isNotBlank();
        verify(valueOperations).set(eq("RT:42"), anyString(), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void RT가_null이면_REFRESH_TOKEN_NOT_FOUND_예외가_발생한다() {
        assertThatThrownBy(() -> authService.reissue(null, new MockHttpServletResponse()))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    @Test
    void 변조된_RT면_INVALID_REFRESH_TOKEN_예외가_발생한다() {
        assertThatThrownBy(() -> authService.reissue("invalid.token", new MockHttpServletResponse()))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void Redis와_RT가_불일치하면_Redis를_삭제하고_예외가_발생한다() {
        String rt = jwtProvider.generateRefreshToken("42");
        given(valueOperations.get("RT:42")).willReturn("other-token");

        assertThatThrownBy(() -> authService.reissue(rt, new MockHttpServletResponse()))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));

        verify(redisTemplate).delete("RT:42");
    }

    // ---- 로그아웃 ----

    @Test
    void 로그아웃시_Redis에서_RT가_삭제되고_쿠키가_만료된다() {
        String rt = jwtProvider.generateRefreshToken("42");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        authService.logout(rt, httpResponse);

        verify(redisTemplate).delete("RT:42");
        String setCookie = httpResponse.getHeader("Set-Cookie");
        assertThat(setCookie).contains("Max-Age=0");
    }

    @Test
    void RT없이_로그아웃해도_예외가_발생하지_않는다() {
        authService.logout(null, new MockHttpServletResponse());
        verify(redisTemplate, never()).delete(anyString());
    }

    // ---- 헬퍼 ----

    private KakaoUserInfoResponse kakaoUserInfo(Long id, String nickname, String email, String imageUrl) {
        KakaoUserInfoResponse.KakaoAccount.Profile profile =
            new KakaoUserInfoResponse.KakaoAccount.Profile(nickname, imageUrl);
        KakaoUserInfoResponse.KakaoAccount account =
            new KakaoUserInfoResponse.KakaoAccount(email, profile);
        return new KakaoUserInfoResponse(id, account);
    }

    private User user(String kakaoId, String nickname, String imageUrl) {
        return User.builder()
            .kakaoId(kakaoId)
            .nickname(nickname)
            .email("test@kakao.com")
            .profileImageUrl(imageUrl)
            .build();
    }
}

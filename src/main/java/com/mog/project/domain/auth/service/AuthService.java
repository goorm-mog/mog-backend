package com.mog.project.domain.auth.service;

import com.mog.project.domain.auth.dto.request.KakaoLoginRequest;
import com.mog.project.domain.auth.dto.response.AccessTokenReissueResponse;
import com.mog.project.domain.auth.dto.response.AuthLoginResponse;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.auth.jwt.JwtProvider;
import com.mog.project.global.auth.oauth2.client.KakaoApiClient;
import com.mog.project.global.auth.oauth2.dto.KakaoUserInfoResponse;
import com.mog.project.global.exception.AuthException;
import com.mog.project.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String RT_PREFIX = "RT:";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    // RT 쿠키 경로를 /api/v1/auth로 제한 → 다른 API 호출 시 RT 쿠키를 전송하지 않아 노출 최소화
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final KakaoApiClient kakaoApiClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${cookie.secure:true}")
    private boolean cookieSecure;

    @Transactional
    public AuthLoginResponse kakaoLogin(KakaoLoginRequest request, HttpServletResponse response) {
        KakaoUserInfoResponse kakaoUser = kakaoApiClient.getUserInfo(request.accessToken());
        String kakaoId = String.valueOf(kakaoUser.id());

        User user = userRepository.findByKakaoId(kakaoId)
            .map(existing -> {
                existing.updateProfile(kakaoUser.getNickname(), kakaoUser.getProfileImageUrl());
                return existing;
            })
            .orElseGet(() -> userRepository.save(
                User.builder()
                    .kakaoId(kakaoId)
                    .nickname(kakaoUser.getNickname())
                    .email(kakaoUser.getEmail())
                    .profileImageUrl(kakaoUser.getProfileImageUrl())
                    .build()
            ));

        String accessToken = jwtProvider.generateAccessToken(kakaoId);
        String refreshToken = jwtProvider.generateRefreshToken(kakaoId);

        saveRefreshToken(kakaoId, refreshToken);
        setRefreshTokenCookie(response, refreshToken);

        return new AuthLoginResponse(
            accessToken,
            new AuthLoginResponse.UserInfo(user.getUserId(), user.getNickname(), user.getProfileImageUrl())
        );
    }

    public AccessTokenReissueResponse reissue(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null) {
            throw new AuthException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (!jwtProvider.isValid(refreshToken)) {
            throw new AuthException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String kakaoId = jwtProvider.getUserId(refreshToken);
        String savedToken = redisTemplate.opsForValue().get(RT_PREFIX + kakaoId);

        if (!refreshToken.equals(savedToken)) {
            // Redis의 RT와 불일치 → RT 탈취 후 재사용 공격 가능성, 즉시 무효화
            redisTemplate.delete(RT_PREFIX + kakaoId);
            throw new AuthException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // RTR: AT와 RT를 모두 교체하여 단발성 RT 유지
        String newAccessToken = jwtProvider.generateAccessToken(kakaoId);
        String newRefreshToken = jwtProvider.generateRefreshToken(kakaoId);

        saveRefreshToken(kakaoId, newRefreshToken);
        setRefreshTokenCookie(response, newRefreshToken);

        return new AccessTokenReissueResponse(newAccessToken);
    }

    public void logout(String refreshToken, HttpServletResponse response) {
        if (refreshToken != null && jwtProvider.isValid(refreshToken)) {
            String kakaoId = jwtProvider.getUserId(refreshToken);
            redisTemplate.delete(RT_PREFIX + kakaoId);
        }
        expireRefreshTokenCookie(response);
    }

    private void saveRefreshToken(String kakaoId, String refreshToken) {
        redisTemplate.opsForValue().set(
            RT_PREFIX + kakaoId,
            refreshToken,
            jwtProvider.getRefreshTokenExpiry(),
            TimeUnit.MILLISECONDS
        );
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
            .httpOnly(true)      // JS 접근 차단 → XSS 방어
            .secure(cookieSecure) // HTTPS에서만 전송 (dev: false, prod: true)
            .path(COOKIE_PATH)
            .maxAge(jwtProvider.getRefreshTokenExpiry() / 1000)
            .sameSite("Strict")  // 동일 사이트 요청에만 쿠키 전송 → CSRF 방어
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void expireRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(cookieSecure)
            .path(COOKIE_PATH)
            .maxAge(0)
            .sameSite("Strict")
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}

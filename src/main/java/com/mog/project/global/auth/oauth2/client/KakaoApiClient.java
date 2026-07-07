package com.mog.project.global.auth.oauth2.client;

import com.mog.project.global.auth.oauth2.dto.KakaoUserInfoResponse;
import com.mog.project.global.exception.AuthException;
import com.mog.project.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoApiClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoApiClient.class);
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;

    public KakaoApiClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
        try {
            KakaoUserInfoResponse kakaoUser = restClient.get()
                .uri(KAKAO_USER_INFO_URL)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(KakaoUserInfoResponse.class);

            validateRequiredAccountInfo(kakaoUser);
            return kakaoUser;
        } catch (HttpClientErrorException e) {
            // 401/403: 카카오 토큰 만료 또는 잘못된 토큰
            log.warn("카카오 사용자 정보 조회 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AuthException(ErrorCode.INVALID_KAKAO_TOKEN);
        } catch (RestClientException e) {
            // 카카오 서버 통신 실패
            log.warn("카카오 서버 통신 실패", e);
            throw new AuthException(ErrorCode.UNAUTHORIZED_USER);
        }
    }

    // 필수 동의항목(닉네임/이메일/프로필사진)이 응답에 없으면 쓰레기 유저 생성 대신 즉시 실패 처리
    private void validateRequiredAccountInfo(KakaoUserInfoResponse kakaoUser) {
        if (kakaoUser.getNickname() == null || kakaoUser.getNickname().isBlank()
            || kakaoUser.getEmail() == null
            || kakaoUser.getProfileImageUrl() == null) {
            log.warn("카카오 필수 동의항목 누락: id={}, nickname={}, email={}, profileImageUrl={}",
                kakaoUser.id(), kakaoUser.getNickname(), kakaoUser.getEmail(), kakaoUser.getProfileImageUrl());
            throw new AuthException(ErrorCode.KAKAO_ACCOUNT_INFO_MISSING);
        }
    }
}

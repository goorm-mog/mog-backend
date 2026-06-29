package com.mog.project.global.auth.oauth2.client;

import com.mog.project.global.auth.oauth2.dto.KakaoUserInfoResponse;
import com.mog.project.global.exception.AuthException;
import com.mog.project.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoApiClient {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;

    public KakaoApiClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
        try {
            return restClient.get()
                .uri(KAKAO_USER_INFO_URL)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(KakaoUserInfoResponse.class);
        } catch (HttpClientErrorException e) {
            // 401/403: 카카오 토큰 만료 또는 잘못된 토큰
            throw new AuthException(ErrorCode.INVALID_KAKAO_TOKEN);
        } catch (RestClientException e) {
            // 카카오 서버 통신 실패
            throw new AuthException(ErrorCode.UNAUTHORIZED_USER);
        }
    }
}

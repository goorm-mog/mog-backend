package com.mog.project.global.auth.oauth2.client;

import com.mog.project.global.exception.AuthException;
import com.mog.project.global.exception.ErrorCode;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class KakaoOAuthClient {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";

    private final RestClient restClient;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    public KakaoOAuthClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public record KakaoTokenResponse(String accessToken, String refreshToken, Long expiresIn) {
    }

    public KakaoTokenResponse exchangeCodeForToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        try {
            Map<String, Object> response = restClient.post()
                .uri(KAKAO_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

            return new KakaoTokenResponse(
                (String) response.get("access_token"),
                (String) response.get("refresh_token"),
                response.get("expires_in") == null ? null : ((Number) response.get("expires_in")).longValue()
            );
        } catch (RestClientResponseException e) {
            // 카카오가 반환한 실제 에러(KOE 코드) 확인용
            log.error("Kakao token exchange failed: status={}, body={}, redirectUri={}",
                e.getStatusCode(), e.getResponseBodyAsString(), redirectUri);
            throw new AuthException(ErrorCode.INVALID_KAKAO_TOKEN);
        } catch (RestClientException e) {
            log.error("Kakao token exchange request failed (no response)", e);
            throw new AuthException(ErrorCode.INVALID_KAKAO_TOKEN);
        }
    }
}

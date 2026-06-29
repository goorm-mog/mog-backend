package com.mog.project.global.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.mog.project.global.auth.oauth2.client.KakaoApiClient;
import com.mog.project.global.auth.oauth2.dto.KakaoUserInfoResponse;
import com.mog.project.global.exception.AuthException;
import com.mog.project.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(KakaoApiClient.class)
class KakaoApiClientTest {

    @Autowired
    private KakaoApiClient kakaoApiClient;

    @Autowired
    private MockRestServiceServer mockServer;

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Test
    void 유효한_토큰으로_유저정보를_가져온다() {
        mockServer.expect(requestTo(KAKAO_USER_INFO_URL))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer valid-token"))
            .andRespond(withSuccess("""
                {
                  "id": 123456789,
                  "kakao_account": {
                    "email": "test@kakao.com",
                    "profile": {
                      "nickname": "테스트유저",
                      "profile_image_url": "http://image.url"
                    }
                  }
                }
                """, MediaType.APPLICATION_JSON));

        KakaoUserInfoResponse result = kakaoApiClient.getUserInfo("valid-token");

        assertThat(result.id()).isEqualTo(123456789L);
        assertThat(result.getNickname()).isEqualTo("테스트유저");
        assertThat(result.getEmail()).isEqualTo("test@kakao.com");
        assertThat(result.getProfileImageUrl()).isEqualTo("http://image.url");
    }

    @Test
    void 만료된_토큰이면_INVALID_KAKAO_TOKEN_예외가_발생한다() {
        mockServer.expect(requestTo(KAKAO_USER_INFO_URL))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> kakaoApiClient.getUserInfo("expired-token"))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_KAKAO_TOKEN));
    }

    @Test
    void 카카오_서버_오류면_UNAUTHORIZED_USER_예외가_발생한다() {
        mockServer.expect(requestTo(KAKAO_USER_INFO_URL))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> kakaoApiClient.getUserInfo("any-token"))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_USER));
    }
}

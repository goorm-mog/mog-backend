package com.mog.project.global.auth.oauth2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(
    Long id,
    @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
        String email,
        Profile profile
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Profile(
            String nickname,
            @JsonProperty("profile_image_url") String profileImageUrl
        ) {}
    }

    public String getNickname() {
        if (kakaoAccount == null || kakaoAccount.profile() == null) return "";
        return kakaoAccount.profile().nickname();
    }

    public String getEmail() {
        if (kakaoAccount == null) return null;
        return kakaoAccount.email();
    }

    public String getProfileImageUrl() {
        if (kakaoAccount == null || kakaoAccount.profile() == null) return null;
        return kakaoAccount.profile().profileImageUrl();
    }
}

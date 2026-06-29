package com.mog.project.domain.auth.dto.response;

public record AuthLoginResponse(
    String accessToken,
    UserInfo user
) {
    public record UserInfo(
        Long userId,
        String nickname,
        String profileImageUrl
    ) {}
}

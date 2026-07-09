package com.mog.project.domain.user.entity;

import com.mog.project.global.common.BaseTimeEntity;
import com.mog.project.global.config.AccountEncryptionConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String kakaoId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String profileImageUrl;

    @Convert(converter = AccountEncryptionConverter.class)
    @Column(length = 500)
    private String kakaoAccessToken;

    // 톡캘린더 등 만료된 accessToken 재발급용 (authorization_code 로그인 시에만 발급됨)
    @Convert(converter = AccountEncryptionConverter.class)
    @Column(length = 500)
    private String kakaoRefreshToken;

    private LocalDateTime kakaoTokenExpiresAt;

    private LocalDateTime deletedAt;

    // 외부에서 User 생성 시 Builder 패턴 사용 강제 (실수로 필드 빠뜨리는 것을 방지)
    @Builder
    public User(String kakaoId, String nickname, String email, String profileImageUrl,
        String kakaoAccessToken, String kakaoRefreshToken, LocalDateTime kakaoTokenExpiresAt) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.kakaoAccessToken = kakaoAccessToken;
        this.kakaoRefreshToken = kakaoRefreshToken;
        this.kakaoTokenExpiresAt = kakaoTokenExpiresAt;
    }

    // 카카오 프로필 바뀌었을 때 동기화용 메서드
    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    // 로그인마다 accessToken 갱신. refreshToken은 authorization_code 로그인일 때만 내려오므로
    // 있을 때만 갱신하고, 없으면(access_token 단독 로그인) 기존 값 유지
    public void updateKakaoToken(String kakaoAccessToken, String kakaoRefreshToken, LocalDateTime kakaoTokenExpiresAt) {
        this.kakaoAccessToken = kakaoAccessToken;
        if (kakaoRefreshToken != null) {
            this.kakaoRefreshToken = kakaoRefreshToken;
            this.kakaoTokenExpiresAt = kakaoTokenExpiresAt;
        }
    }
}

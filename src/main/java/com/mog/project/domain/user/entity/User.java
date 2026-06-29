package com.mog.project.domain.user.entity;

import com.mog.project.global.common.BaseTimeEntity;
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

    private LocalDateTime deletedAt;

    // 외부에서 User 생성 시 Builder 패턴 사용 강제 (실수로 필드 빠뜨리는 것을 방지)
    @Builder
    public User(String kakaoId, String nickname, String email, String profileImageUrl) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }

    // 카카오 프로필 바뀌었을 때 동기화용 메서드
    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }
}

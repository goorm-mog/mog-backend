package com.mog.project.domain.groups.entity;

import com.mog.project.global.common.BaseTimeEntity;        
import jakarta.persistence.*;                               
import java.time.LocalDateTime;                             
import lombok.*;  

@Entity
@Table(name = "groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @Column(nullable = false, length = 20)
    private String groupName;

    @Column(nullable = false, unique = true, length = 6)
    private String inviteCode;

    @Column(length = 255)
    private String kakaoShareUrl;

    private LocalDateTime deletedAt;

    @Builder
    public Group(String groupName, String inviteCode, String kakaoShareUrl) {
        this.groupName = groupName;
        this.inviteCode = inviteCode;
        this.kakaoShareUrl = kakaoShareUrl;
    }
}

package com.mog.project.domain.groups.entity;

import com.mog.project.domain.user.entity.User;
import jakarta.persistence.*;                               
import java.time.LocalDateTime;                             
import lombok.*; 

@Entity
@Table(
    name = "group_members",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_groupmembers_group_user",
        columnNames = {"group_id", "user_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GroupMemberRole role;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Builder
    public GroupMember(Group group, User user, GroupMemberRole role, LocalDateTime joinedAt)
    {
        this.group = group;
        this.user = user;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }
}

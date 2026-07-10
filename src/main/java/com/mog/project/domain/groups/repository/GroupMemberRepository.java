package com.mog.project.domain.groups.repository;

import com.mog.project.domain.groups.entity.GroupMember;
import com.mog.project.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface GroupMemberRepository extends JpaRepository<GroupMember, Long>
{
    // 중복 가입 방지
    boolean existsByGroupGroupIdAndUserUserId(Long groupId, Long userId);

    // 그룹 조회
    @Query("SELECT gm FROM GroupMember gm WHERE gm.user.userId = :userId AND gm.group.deletedAt IS NULL")
    List<GroupMember> findActiveGroupsByUserId(@Param("userId") Long userId);
    int countByGroupGroupId(Long groupId);

    // 그룹 수정/삭제
    Optional<GroupMember> findByGroupGroupIdAndUserUserId(Long groupId, Long userId);

    // 그룹 상세
    List<GroupMember> findByGroupGroupId(Long groupId);

    // 그룹 삭제 시 소속 멤버 전체 정리
    void deleteAllByGroupGroupId(Long groupId);

    Long user(User user);
}
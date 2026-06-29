package com.mog.project.domain.groups.repository;

import com.mog.project.domain.groups.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;                                            

import java.util.List;
import java.util.Optional;


public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> 
{
    // 중복 가입 방지
    boolean existsByGroupGroupIdAndUserUserId(Long groupId, Long userId);
    
    // 그룹 조회
    List<GroupMember> findByUserUserId(Long userId);
    int countByGroupGroupId(Long groupId);

    // 그룹 수정
    Optional<GroupMember> findByGroupGroupIdAndUserUserId(Long groupId, Long userId);
}
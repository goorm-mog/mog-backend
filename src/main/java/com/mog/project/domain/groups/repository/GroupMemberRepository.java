package com.mog.project.domain.groups.repository;

import com.mog.project.domain.groups.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;                                            

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> 
{                                              
    boolean existsByGroupGroupIdAndUserUserId(Long groupId, Long userId);
    
    List<GroupMember> findByUserUserId(Long userId);
    int countByGroupGroupId(Long groupId);
}
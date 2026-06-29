package com.mog.project.domain.groups.repository;

import com.mog.project.domain.groups.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;      
                                                            
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> 
{                                              
    boolean existsByGroupGroupIdAndUserUserId(Long groupId, Long userId);
}
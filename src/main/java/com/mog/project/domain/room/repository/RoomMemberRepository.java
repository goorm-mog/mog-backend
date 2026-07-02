package com.mog.project.domain.room.repository;

import com.mog.project.domain.room.entity.RoomMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    List<RoomMember> findByRoomRoomId(Long roomId);    
}

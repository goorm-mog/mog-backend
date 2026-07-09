package com.mog.project.domain.room.repository;

import com.mog.project.domain.room.entity.RoomMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    List<RoomMember> findByRoomRoomId(Long roomId);    

    // 방 ID + 유저 ID 조합으로 이 사람이 방 멤버인지 존재여부 체크
    boolean existsByRoomRoomIdAndUserUserId(Long roomId, Long userId);
}

package com.mog.project.domain.room.repository;

import com.mog.project.domain.room.entity.Room;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByGroupGroupIdAndDeletedAtIsNull(Long groupId);
}

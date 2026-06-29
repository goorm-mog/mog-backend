package com.mog.project.meeting.repository;

import com.mog.project.meeting.entity.RoomPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomPhotoRepository extends JpaRepository<RoomPhoto, Long> {

    // 해당 방의 사진 목록 전체 조회
    List<RoomPhoto> findByRoomId(Long roomId);

    // 사진 업로드 전 3장인지 확인
    int countByRoomId(Long roomId);

    // 다른 방의 사진을 삭제하는 것을 방지 하기 위해, roomId와 사진 Id를 받아 이중 확인
    Optional<RoomPhoto> findByIdAndRoomId(Long id, Long roomId);
}

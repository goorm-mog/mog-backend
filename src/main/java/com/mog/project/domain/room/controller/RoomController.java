package com.mog.project.domain.room.controller;

import com.mog.project.domain.room.dto.request.RoomCreateRequest;
import com.mog.project.domain.room.dto.response.RoomCreateResponse;
import com.mog.project.domain.room.dto.response.RoomListResponse;
import com.mog.project.domain.room.service.RoomService;
import com.mog.project.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Room", description = "방 API")
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor

public class RoomController {

    private final RoomService roomService;

    @Operation(
        summary = "방 생성",
        description = "그룹 멤버가 약속 방을 생성합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{group}/rooms")
    public ResponseEntity<ApiResponse<RoomCreateResponse>> creatRoom(
        @AuthenticationPrincipal String kakaoId,
        @PathVariable Long groupId,
        @Valid @RequestBody RoomCreateRequest request
    ) {
        RoomCreateResponse response = roomService.createRoom(kakaoId, groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ROOM_CREATE_SUCCESS", "약속 방이 성공적으로 생성되었습니다.", response));
    }
    
    @Operation(
        summary = "방 조회",
        description = "그룹 멤버가 약속 방들을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{groupId}/rooms")
    public ResponseEntity<ApiResponse<RoomListResponse>> getRoomList(
        @AuthenticationPrincipal String kakaoId,
        @PathVariable Long groupId
    ) {
        RoomListResponse response = roomService.getRoomList(kakaoId, groupId);
        return ResponseEntity.ok(
            ApiResponse.success("ROOM_LIST_FETCH_SUCCESS", "그룹 내 약속 방 목록을 성공적으로 조회했습니다.", response)
        );
    }
}

package com.mog.project.domain.groups.controller;
                                                            
import com.mog.project.domain.groups.dto.request.GroupCreateRequest;                                                    
import com.mog.project.domain.groups.dto.response.GroupCreateResponse;   
import com.mog.project.domain.groups.dto.request.GroupJoinRequest;
import com.mog.project.domain.groups.dto.response.GroupJoinResponse;   
import com.mog.project.domain.groups.dto.response.GroupListResponse;  
import com.mog.project.domain.groups.dto.request.GroupUpdateRequest;
import com.mog.project.domain.groups.dto.response.GroupUpdateResponse;
import com.mog.project.domain.groups.dto.response.GroupDeleteResponse;
import com.mog.project.domain.groups.dto.response.GroupDetailResponse;
import com.mog.project.domain.groups.dto.response.GroupLeaveResponse;  
import com.mog.project.domain.groups.service.GroupService;
import com.mog.project.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;             
import io.swagger.v3.oas.annotations.security.SecurityRequirement; 
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;                            
import lombok.RequiredArgsConstructor;      
import org.springframework.http.HttpStatus;                 
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;                            
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Group", description = "그룹 API")
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(
        summary = "그룹 생성", 
        description = "그룹을 생성합니다. 생성자는 자동으로 LEADER로 설정됩니다.", 
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<ApiResponse<GroupCreateResponse>> createGroup(
            @AuthenticationPrincipal String kakaoId,
            @Valid @RequestBody GroupCreateRequest request
    ) {
        GroupCreateResponse response = groupService.createGroup(kakaoId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("GROUP_CREATE_SUCCESS", "그룹이 성공적으로 생성되었습니다", response));
    }

    @Operation(
        summary = "그룹 참여", 
        description = "초대 코드로 그룹에 참여합니다.",                                       
        security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<GroupJoinResponse>> joinGroup(
        @AuthenticationPrincipal String kakaoId,
        @Valid @RequestBody GroupJoinRequest request
    ) {
        GroupJoinResponse response = groupService.joinGroup(kakaoId, request);
        return ResponseEntity.ok(ApiResponse.success("GROUP_JOIN_SUCCESS", "그룹 참여에 성공했습니다.", response));
    }

    @Operation(
        summary = "내 그룹 목록 조회",
        description = "내가 속한 그룹 목록을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
        public ResponseEntity<ApiResponse<GroupListResponse>>
        getMyGroups(
            @AuthenticationPrincipal String kakaoId
        ) {
            GroupListResponse response = groupService.getMyGroups(kakaoId);
            return ResponseEntity.ok(ApiResponse.success("GROUP_LIST_FETCH_SUCCESS", "참여 중인 그룹 목록을 성공적으로 조회했습니다.", response));
    }

    @Operation(summary = "그룹 상세 조회", description = "그룹 상세 정보 및 멤버/방 목록을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupDetailResponse>> getGroupDetail(
        @AuthenticationPrincipal String kakaoId,
        @PathVariable Long groupId
    ) {
        GroupDetailResponse response = groupService.getGroupDetail(kakaoId, groupId);
        return ResponseEntity.ok(ApiResponse.success("GROUP_DETAIL_FETCH_SUCCESS", "그룹 상세 정보를 성공적으로 조회했습니다.", response));
    }

    @Operation(
        summary = "그룹 수정",
        description = "그룹 이름을 수정합니다. LEADER만 가능합니다.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{groupId}")
      public ResponseEntity<ApiResponse<GroupUpdateResponse>>
      updateGroup(
          @AuthenticationPrincipal String kakaoId,
          @PathVariable Long groupId,
          @Valid @RequestBody GroupUpdateRequest request
        ) {
            GroupUpdateResponse response = groupService.updateGroup(kakaoId, groupId, request);
            return ResponseEntity.ok(ApiResponse.success("GROUP_UPDATE_SUCCESS", "그룹 이름이 성공적으로 변경되었습니다.", response));
      }

    @Operation(summary = "그룹 삭제", description = "그룹을 삭제합니다. LEADER만 가능합니다.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupDeleteResponse>> deleteGroup(
        @AuthenticationPrincipal String kakaoId,
        @PathVariable Long groupId
    ) {
        GroupDeleteResponse response = groupService.deleteGroup(kakaoId, groupId);
        return ResponseEntity.ok(ApiResponse.success("GROUP_DELETE_SUCCESS", "그룹 및 하위 방들이 소프트 삭제 처리되었습니다.", response));
    }

    @Operation(summary = "그룹 탈퇴", description = "그룹에서 탈퇴합니다. LEADER는 탈퇴할 수 없습니다.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<ApiResponse<GroupLeaveResponse>> leaveGroup(
        @AuthenticationPrincipal String kakaoId,
        @PathVariable Long groupId
    ) {
        GroupLeaveResponse response = groupService.leaveGroup(kakaoId, groupId);
        return ResponseEntity.ok(ApiResponse.success("GROUP_LEAVE_SUCCESS", "그룹에서 성공적으로 탈퇴했습니다.", response));
    }
}
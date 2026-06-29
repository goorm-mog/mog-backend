package com.mog.project.domain.groups.controller;
                                                            
import com.mog.project.domain.groups.dto.request.GroupCreateRequest;                                                    
import com.mog.project.domain.groups.dto.response.GroupCreateResponse;                                                  
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
}
package com.mog.project.domain.meeting.controller;


import com.mog.project.global.response.ApiResponse;
import com.mog.project.domain.meeting.dto.response.RoomPhotoResponse;
import com.mog.project.domain.meeting.service.RoomPhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@Tag(name = "약속 사진", description = "약속 사진 업로드 / 삭제 API")
@RestController
@RequestMapping("/api/v1/rooms/{roomId}/photos")
@RequiredArgsConstructor
public class RoomPhotoController {

    private final RoomPhotoService roomPhotoService;
    @Operation(
            summary = "사진 업로드",
            description = "방에 사진을 업로드합니다.\n\n" +
                    "- 허용 형식: **jpeg, png, webp**\n" +
                    "- 최대 파일 크기: **10MB**\n" +
                    "- 방당 최대 **3장**까지 등록 가능\n" +
                    "- `Content-Type: multipart/form-data` 로 요청해야 합니다\n" +
                    "- form-data key 이름: `image`"
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RoomPhotoResponse>> uploadPhoto(
            @Parameter(description = "방 ID", example = "1") @PathVariable Long roomId,
            @Parameter(description = "업로드할 이미지 파일 (jpeg/png/webp, 최대 10MB)")
            @RequestParam("image") MultipartFile image
    ) {
        return ResponseEntity.status(201).body(
                ApiResponse.success("사진을 업로드했습니다.", roomPhotoService.uploadPhoto(roomId, image))
        );
    }

    @Operation(
            summary = "사진 삭제",
            description = "방에 등록된 사진을 삭제합니다.\n\n" +
                    "- S3에서도 함께 삭제됩니다\n" +
                    "- 다른 방의 사진은 삭제할 수 없습니다"
    )
    @DeleteMapping("/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @Parameter(description = "방 ID", example = "1") @PathVariable Long roomId,
            @Parameter(description = "삭제할 사진 ID", example = "5") @PathVariable Long photoId
    ) {
        roomPhotoService.deletePhoto(roomId, photoId);
        return ResponseEntity.ok(ApiResponse.success("사진을 삭제했습니다."));
    }



}

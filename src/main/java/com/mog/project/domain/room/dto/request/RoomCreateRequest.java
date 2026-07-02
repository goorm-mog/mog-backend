package com.mog.project.domain.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCreateRequest(
    @NotBlank
    @Size(min = 1, max = 50)
    String roomName
) {}
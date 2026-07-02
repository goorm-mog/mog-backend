package com.mog.project.domain.room.dto.request;

import com.mog.project.domain.room.entity.RoomStatus;
import jakarta.validation.constraints.NotNull;

public record RoomStepRequest(
    @NotNull
    RoomStatus nextStatus
) {}

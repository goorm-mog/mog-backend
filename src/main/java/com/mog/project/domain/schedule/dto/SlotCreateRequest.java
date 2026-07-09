package com.mog.project.domain.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
 
public record SlotCreateRequest(
        List<SlotItem> slots
) {
    public record SlotItem(
            LocalDate date,
            LocalTime time
    ) {}
}
 
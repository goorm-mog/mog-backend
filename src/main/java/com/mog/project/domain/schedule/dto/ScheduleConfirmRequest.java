package com.mog.project.domain.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
 
public record ScheduleConfirmRequest(
        LocalDate date,
        LocalTime time
) {}
 
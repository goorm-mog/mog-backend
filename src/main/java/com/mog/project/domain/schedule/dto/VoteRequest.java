package com.mog.project.domain.schedule.dto;

import java.util.List;
 
public record VoteRequest(
        List<Long> slotIds
) {}
 
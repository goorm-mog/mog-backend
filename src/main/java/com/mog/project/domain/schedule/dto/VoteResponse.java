package com.mog.project.domain.schedule.dto;

import java.util.List;
 
public record VoteResponse(
        List<Long> votedSlotIds
) {}
 
package com.mog.project.domain.schedule.dto;

import com.mog.project.domain.schedule.entity.ScheduleSlot;
import com.mog.project.domain.schedule.entity.ScheduleVote;
 
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
 
public record SlotListResponse(
        Long roomId,
        List<SlotItem> slots
) {
    public record SlotItem(
            Long slotId,
            LocalDate date,
            LocalTime time,
            int voteCount,
            List<Long> votedUserIds
    ) {}
 
    public static SlotListResponse from(Long roomId, List<ScheduleSlot> slots, List<ScheduleVote> votes) {
        // slotId 기준으로 투표 목록 그룹핑
        Map<Long, List<ScheduleVote>> voteMap = votes.stream()
                .collect(Collectors.groupingBy(ScheduleVote::getSlotId));
 
        List<SlotItem> slotItems = slots.stream()
                .map(slot -> {
                    List<ScheduleVote> slotVotes = voteMap.getOrDefault(slot.getId(), List.of());
                    List<Long> votedUserIds = slotVotes.stream()
                            .map(ScheduleVote::getUserId)
                            .toList();
                    return new SlotItem(
                            slot.getId(),
                            slot.getSlotDate(),
                            slot.getSlotTime(),
                            votedUserIds.size(),
                            votedUserIds
                    );
                })
                .toList();
 
        return new SlotListResponse(roomId, slotItems);
    }
}
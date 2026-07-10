package com.mog.project.domain.meeting.dto.response;

import com.mog.project.domain.meeting.entity.MeetingMenuItem;

public record MenuItemResponse(
        Long id,
        String itemName,
        Integer quantity,
        Integer price,
        Integer totalPrice
) {
    public static MenuItemResponse from(MeetingMenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getItemName(),
                item.getQuantity(),
                item.getPrice(),
                item.getQuantity() * item.getPrice()
        );
    }
}

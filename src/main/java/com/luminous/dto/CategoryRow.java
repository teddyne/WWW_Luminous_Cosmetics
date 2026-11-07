package com.luminous.dto;

import com.luminous.model.enums.CategoryStatus;

import java.time.LocalDateTime;

public record CategoryRow(
        Integer id,
        String name,
        String description,
        CategoryStatus status,
        long productCount,
        LocalDateTime createdAt) {

    public boolean active() {
        return status == CategoryStatus.ACTIVE;
    }
}

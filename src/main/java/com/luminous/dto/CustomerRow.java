package com.luminous.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerRow(
        Integer id,
        String fullName,
        String email,
        String phone,
        int pointBalance,
        boolean active,
        String roleName,
        LocalDateTime createdAt,
        long orderCount,
        BigDecimal totalSpent,
        Long version) {

    public String totalSpentText() {
        return Vnd.format(totalSpent);
    }

    public boolean admin() {
        return "ROLE_ADMIN".equals(roleName);
    }
}

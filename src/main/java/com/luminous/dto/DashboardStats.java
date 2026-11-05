package com.luminous.dto;

import java.math.BigDecimal;

public record DashboardStats(
        BigDecimal revenue,
        long newOrders,
        long newCustomers,
        long totalProducts) {

    public String revenueText() {
        return Vnd.format(revenue);
    }
}

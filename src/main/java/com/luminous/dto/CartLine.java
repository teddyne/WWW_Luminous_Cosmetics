package com.luminous.dto;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** 1 dong trong gio hang: the san pham + so luong + thanh tien cua dong do. */
public record CartLine(ProductCard card, int quantity, BigDecimal lineTotal) {

    /** Con hang de mua hay khong. Het hang thi chi hien, khong cho chon/thanh toan. */
    public boolean available() {
        return card.getVariant() != null
                && card.getVariant().getStockQuantity() != null
                && card.getVariant().getStockQuantity() > 0;
    }

    public String lineTotalText() {
        return formatVnd(lineTotal);
    }

    /** Dinh dang tien Viet: 245.000đ. */
    public static String formatVnd(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(value) + "đ";
    }
}

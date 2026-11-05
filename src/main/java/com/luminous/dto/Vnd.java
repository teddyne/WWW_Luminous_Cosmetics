package com.luminous.dto;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class Vnd {

    private Vnd() {
    }

    public static String format(BigDecimal value) {
        if (value == null) {
            return "0đ";
        }
        return NumberFormat.getInstance(Locale.of("vi", "VN")).format(value) + "đ";
    }

    public static String format(long value) {
        return NumberFormat.getInstance(Locale.of("vi", "VN")).format(value);
    }
}

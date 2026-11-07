package com.luminous.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

/**
 * 1 buoc trong chu trinh, da giai tu routineJson + variant that trong DB.
 * Template KHONG tu tinh gia hay tu doan loai da phu hop.
 */
@Getter
@AllArgsConstructor
public class RoutineStep {
    private final int stepIndex;          // vi tri trong routineJson, dung khi doi san pham
    private final String time;            // MORNING / EVENING
    private final String timeLabel;       // Buoi sang / Buoi toi
    private final Integer categoryId;
    private final String categoryName;
    private final ProductCard card;       // ten, gia, anh, sao, da ban (tai su dung tu trang chu)
    private final boolean exactFit;       // true = variant ghi phu hop dung loai da nay
    private final String suitableText;    // vi du "Da dau, Da hon hop" (dich tu suitableSkinType)
    private final String usage;           // huong dan dung o buoc nay
    private final List<ProductCard> alternatives; // cung danh muc, khac variant hien tai

    public String stepKey() {
        return time + "-" + categoryId;
    }
}

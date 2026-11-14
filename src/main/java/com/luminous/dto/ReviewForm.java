package com.luminous.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Form danh gia 1 san pham trong don (moi dong OrderDetail 1 lan). */
@Getter
@Setter
public class ReviewForm {

    @NotNull
    private Integer detailId;

    @NotNull(message = "Vui lòng chọn số sao")
    @Min(value = 1, message = "Số sao từ 1 đến 5")
    @Max(value = 5, message = "Số sao từ 1 đến 5")
    private Byte rating;

    private String comment;

    private boolean anonymous;
}

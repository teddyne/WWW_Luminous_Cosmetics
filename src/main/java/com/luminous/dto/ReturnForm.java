package com.luminous.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Form yeu cau tra hang / hoan tien (trang Don mua -> form rieng). */
@Getter
@Setter
public class ReturnForm {

    @NotBlank(message = "Vui lòng chọn lý do trả hàng")
    private String reason;

    /** Ghi chu them (tinh trang san pham, dia chi nhan lai hang...). */
    private String note;

    /** ORIGINAL: hoan ve phuong thuc da thanh toan; BANK: chuyen khoan ngan hang. */
    private String refundMethod = "ORIGINAL";
}

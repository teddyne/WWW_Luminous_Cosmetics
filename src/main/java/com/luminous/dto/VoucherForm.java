package com.luminous.dto;

import com.luminous.model.Voucher;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VoucherForm {

    private Integer id;

    @NotBlank(message = "Tên chương trình không được để trống")
    @Size(max = 100, message = "Tên chương trình tối đa 100 ký tự")
    private String voucherName;

    @Size(max = 50, message = "Mã voucher tối đa 50 ký tự")
    private String voucherCode;

    private String description;

    @NotBlank(message = "Vui lòng chọn hình thức giảm")
    private String discountType = "PERCENTAGE";

    @NotNull(message = "Mức giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Mức giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.01", message = "Mức giảm tối đa phải lớn hơn 0")
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Giá trị đơn tối thiểu không được để trống")
    @DecimalMin(value = "0", message = "Giá trị đơn tối thiểu không được âm")
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @NotNull(message = "Tổng số lượng mã không được để trống")
    @Min(value = 1, message = "Tổng số lượng mã phải từ 1 trở lên")
    private Integer usageLimit = 100;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;

    private String applicableScope = "ALL";

    private String scopeType = "ALL";

    /** Loai voucher hien thi (ten trong bang VoucherType): "Mien phi van chuyen",
     *  "Giam gia theo phan tram", "Giam gia co dinh" hoac "Voucher dac biet".
     *  Mac dinh theo co che giam dang chon; rieng "Voucher dac biet" la lua
     *  chon hien thi rieng (nhan vang) du co che giam van la % hoac so tien. */
    private String voucherType = "Giảm giá theo phần trăm";

    private List<Integer> categoryIds = new ArrayList<>();

    private List<Integer> productIds = new ArrayList<>();

    private Boolean active = true;

    /** Loai the xem truoc (shipping/percent/fixed/special) tu du lieu form. */
    public String previewKind() {
        return Voucher.badgeKind(voucherType,
                com.luminous.model.enums.VoucherApplicableScope.from(applicableScope),
                com.luminous.model.enums.DiscountType.from(discountType));
    }

    /** Nhan in tren the xem truoc tu du lieu form. */
    public String previewLabel() {
        return Voucher.badgeLabel(voucherType,
                com.luminous.model.enums.VoucherApplicableScope.from(applicableScope),
                com.luminous.model.enums.DiscountType.from(discountType), discountValue);
    }
}

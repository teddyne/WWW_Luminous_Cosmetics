package com.luminous.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Form them/sua san pham: 1 san pham co NHIEU bien the (dung tich / mau sac,
 * vi du son co nhieu ma mau) + thong so san pham (key-value) + huong dan
 * su dung. Mo ta chi tiet la HTML (soan bang TinyMCE o form admin).
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductForm {

    private Integer id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
    private String name;

    @Size(max = 100, message = "Thương hiệu tối đa 100 ký tự")
    private String brand;

    @NotNull(message = "Vui lòng chọn loại sản phẩm")
    private Integer categoryId;

    /** Mo ta chi tiet dang HTML (TinyMCE), hien bang th:utext o trang chi tiet. */
    private String description;

    /** Huong dan su dung (van ban thuong, giu xuong dong khi hien). */
    private String usageInstructions;

    /** Bien the: it nhat 1 dong, moi dong co gia rieng, ton kho rieng. */
    @Valid
    @Size(min = 1, message = "Cần ít nhất một biến thể")
    private List<VariantForm> variants = new ArrayList<>(List.of(new VariantForm()));

    /** Thong so san pham (Barcode, Xuat xu...): dong nao trong ten/gia tri thi bo qua. */
    private List<AttributeForm> attributes = new ArrayList<>();

    private String ingredients;

    private List<String> suitableSkinTypes = new ArrayList<>();

    private Boolean active = true;

    @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
    private String imageUrl;

    @Size(max = 500, message = "Đường dẫn video tối đa 500 ký tự")
    private String videoUrl;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class VariantForm {

        /** null = bien the moi; co id = cap nhat bien the da co. */
        private Integer id;

        @Size(max = 50, message = "Mã SKU tối đa 50 ký tự")
        private String sku;

        @Size(max = 50, message = "Dung tích tối đa 50 ký tự")
        private String size;

        @Size(max = 100, message = "Tên màu tối đa 100 ký tự")
        private String shadeName;

        @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "Mã màu phải có dạng #RRGGBB")
        private String hexCode;

        @NotNull(message = "Giá bán không được để trống")
        @DecimalMin(value = "0.01", message = "Giá bán phải lớn hơn 0")
        private BigDecimal price;

        @DecimalMin(value = "0.01", message = "Giá khuyến mãi phải lớn hơn 0")
        private BigDecimal salePrice;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        private Integer stockQuantity = 0;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AttributeForm {

        @Size(max = 100, message = "Tên thông số tối đa 100 ký tự")
        private String name;

        @Size(max = 500, message = "Giá trị thông số tối đa 500 ký tự")
        private String value;

        public AttributeForm(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}

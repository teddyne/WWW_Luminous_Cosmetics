package com.luminous.dto;

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

    private String description;

    @Size(max = 50, message = "Mã sản phẩm tối đa 50 ký tự")
    private String sku;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.01", message = "Giá bán phải lớn hơn 0")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Giá khuyến mãi phải lớn hơn 0")
    private BigDecimal salePrice;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer stockQuantity = 0;

    @Size(max = 50, message = "Dung tích tối đa 50 ký tự")
    private String size;

    @Size(max = 100, message = "Tên màu tối đa 100 ký tự")
    private String shadeName;

    @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "Mã màu phải có dạng #RRGGBB")
    private String hexCode;

    private String ingredients;

    private List<String> suitableSkinTypes = new ArrayList<>();

    private Boolean active = true;

    @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
    private String imageUrl;

    @Size(max = 500, message = "Đường dẫn video tối đa 500 ký tự")
    private String videoUrl;
}

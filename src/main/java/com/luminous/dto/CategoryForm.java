package com.luminous.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategoryForm {

    private Integer id;

    @NotBlank(message = "Tên loại sản phẩm không được để trống")
    @Size(max = 100, message = "Tên loại sản phẩm tối đa 100 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    private String status = "ACTIVE";
}

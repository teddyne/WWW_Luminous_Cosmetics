package com.luminous.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileForm {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^$|^0[0-9]{9}$", message = "Số điện thoại phải có 10 số và bắt đầu bằng 0")
    private String phone;

    /**
     * Gioi tinh o trang Ho so: "" = Khong xac dinh (luu null), NAM / NU.
     * AdminSettingsController dung form nay chi de doi ten + sdt nen bo qua truong nay.
     */
    @Pattern(regexp = "^$|NAM|NU", message = "Giới tính không hợp lệ")
    private String gender;

    /** Ngay/thang/nam sinh o trang Ho so: de trong ca 3 o = khong doi ngay sinh. */
    private String birthDay;
    private String birthMonth;
    private String birthYear;
}

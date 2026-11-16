package com.luminous.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Getter/setter viet tay (khong dung Lombok) de file nay bien dich duoc
 * ngay ca khi annotation processing cua Lombok bi tat o IntelliJ.
 */
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

    private Long version;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getBirthDay() { return birthDay; }
    public void setBirthDay(String birthDay) { this.birthDay = birthDay; }
    public String getBirthMonth() { return birthMonth; }
    public void setBirthMonth(String birthMonth) { this.birthMonth = birthMonth; }
    public String getBirthYear() { return birthYear; }
    public void setBirthYear(String birthYear) { this.birthYear = birthYear; }
}

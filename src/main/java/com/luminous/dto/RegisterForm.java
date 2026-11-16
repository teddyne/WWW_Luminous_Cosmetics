package com.luminous.dto;

import jakarta.validation.constraints.*;

/**
 * Khop dung cac o tren form Dang ky theo Figma:
 * identifier = "email hoac so dien thoai" (1 o duy nhat),
 * mat khau, ho ten, captcha, ngay/thang/nam sinh, gioi tinh, 2 checkbox dieu khoan.
 *
 * Getter/setter viet tay (khong dung Lombok) de file nay bien dich duoc
 * ngay ca khi annotation processing cua Lombok bi tat o IntelliJ.
 */
public class RegisterForm {
    @NotBlank(message = "Vui lòng nhập email hoặc số điện thoại")
    @Size(max = 100, message = "Email/số điện thoại tối đa 100 ký tự")
    private String identifier;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 8, message = "Mật khẩu ít nhất 8 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng nhập họ và tên")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập mã captcha")
    private String captcha;

    /**
     * Ma xac thuc 6 chu so gui qua email/SMS toi dung email/sdt vua nhap.
     * Nhan nut "Gửi mã xác thực" truoc, nhap ma vao day roi moi bam Dang ky.
     */
    @NotBlank(message = "Vui lòng nhập mã xác thực 6 chữ số đã gửi tới email/số điện thoại của bạn")
    @Pattern(regexp = "^\\d{6}$", message = "Mã xác thực gồm đúng 6 chữ số")
    private String verificationCode;

    // De String (khong phai Integer) de o trong "" khong bi loi convert cua Spring,
    // validation se bao bang tieng Viet, AuthService moi parse sang so.
    @NotBlank(message = "Vui lòng chọn ngày sinh")
    private String birthDay;

    @NotBlank(message = "Vui lòng chọn tháng sinh")
    private String birthMonth;

    @NotBlank(message = "Vui lòng chọn năm sinh")
    private String birthYear;

    @NotBlank(message = "Vui lòng chọn giới tính")
    @Pattern(regexp = "NAM|NU", message = "Giới tính không hợp lệ")
    private String gender;

    @AssertTrue(message = "Bạn cần đồng ý với điều kiện chung của Luminous")
    private boolean termsAccepted;

    @AssertTrue(message = "Bạn cần đồng ý với chính sách của Luminous")
    private boolean policyAccepted;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getCaptcha() { return captcha; }
    public void setCaptcha(String captcha) { this.captcha = captcha; }
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    public String getBirthDay() { return birthDay; }
    public void setBirthDay(String birthDay) { this.birthDay = birthDay; }
    public String getBirthMonth() { return birthMonth; }
    public void setBirthMonth(String birthMonth) { this.birthMonth = birthMonth; }
    public String getBirthYear() { return birthYear; }
    public void setBirthYear(String birthYear) { this.birthYear = birthYear; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(boolean termsAccepted) { this.termsAccepted = termsAccepted; }
    public boolean isPolicyAccepted() { return policyAccepted; }
    public void setPolicyAccepted(boolean policyAccepted) { this.policyAccepted = policyAccepted; }
}

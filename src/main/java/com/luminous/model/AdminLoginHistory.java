package com.luminous.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "AdminLoginHistory")
public class AdminLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loginHistoryID")
    private Long id;

    @Column(name = "userID")
    private Integer userId;

    @Column(name = "username", nullable = false, length = 200)
    private String username;

    @Column(name = "method", nullable = false, length = 20)
    private String method;

    @Column(name = "success", nullable = false)
    private boolean success = true;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "ipAddress", length = 45)
    private String ipAddress;

    @Column(name = "userAgent", length = 500)
    private String userAgent;

    @Column(name = "loginAt", nullable = false)
    private LocalDateTime loginAt = LocalDateTime.now();

    public AdminLoginHistory(Integer userId, String username, String method, boolean success,
                             String reason, String ipAddress, String userAgent) {
        this.userId = userId;
        this.username = username;
        this.method = method;
        this.success = success;
        this.reason = reason;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    /** Ten thiet bi de hieu: iPhone, Macbook, May tinh Windows... Trinh duyet chi cho biet loai thiet bi chung, khong cho biet doi may cu the (vi du iPhone 17 hay iPhone 15 deu chi hien "iPhone"). */
    @Transient
    public String getDeviceName() {
        String ua = userAgent == null ? "" : userAgent;
        if (ua.contains("iPhone")) {
            return "iPhone";
        }
        if (ua.contains("iPad")) {
            return "iPad";
        }
        if (ua.contains("Android")) {
            return "Điện thoại Android";
        }
        if (ua.contains("Macintosh") || ua.contains("Mac OS")) {
            return "Macbook";
        }
        if (ua.contains("Windows")) {
            return "Máy tính Windows";
        }
        if (ua.contains("CrOS")) {
            return "Chromebook";
        }
        if (ua.contains("Linux")) {
            return "Máy tính Linux";
        }
        return "Thiết bị khác";
    }

    /** Cach dang nhap viet bang tieng Viet thuong. */
    @Transient
    public String getMethodLabel() {
        return switch (method == null ? "" : method) {
            case "FORM" -> "Mật khẩu";
            case "GOOGLE" -> "Google";
            case "REMEMBER_ME" -> "Tự động ghi nhớ";
            default -> "Khác";
        };
    }
}

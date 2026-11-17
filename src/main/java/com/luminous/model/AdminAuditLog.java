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
@Table(name = "AdminAuditLog")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auditLogID")
    private Long id;

    @Column(name = "userID")
    private Integer userId;

    @Column(name = "username", nullable = false, length = 200)
    private String username;

    @Column(name = "httpMethod", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @Column(name = "handler", length = 200)
    private String handler;

    @Column(name = "status")
    private Integer status;

    @Column(name = "ipAddress", length = 45)
    private String ipAddress;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AdminAuditLog(Integer userId, String username, String httpMethod, String path,
                         String handler, Integer status, String ipAddress) {
        this.userId = userId;
        this.username = username;
        this.httpMethod = httpMethod;
        this.path = path;
        this.handler = handler;
        this.status = status;
        this.ipAddress = ipAddress;
    }

    /**
     * Mo ta viec da lam bang tieng Viet thuong (khong dung thuat ngu ky thuat)
     * de nguoi khong ranh cong nghe van doc duoc: vi du "Them san pham moi",
     * "Cap nhat chuong trinh Flash Sale", "Doi cai dat cua hang".
     */
    @Transient
    public String getDescription() {
        String p = path == null ? "" : path;
        if (p.startsWith("/admin/settings/")) {
            return switch (p.substring("/admin/settings/".length())) {
                case "store" -> "Đổi thông tin cửa hàng";
                case "shipping" -> "Đổi cài đặt vận chuyển";
                case "payment" -> "Đổi cài đặt thanh toán";
                case "ai" -> "Đổi cài đặt Luminous AI";
                case "profile" -> "Đổi thông tin tài khoản";
                case "password" -> "Đổi mật khẩu";
                default -> "Đổi cài đặt cửa hàng";
            };
        }
        String[] parts = p.split("/");
        String section = parts.length > 2 ? parts[2] : "";
        if (section.equals("orders")) {
            return "Cập nhật đơn hàng";
        }
        if (section.equals("customers")) {
            if (p.endsWith("/bulk") || parts.length > 3) {
                return "Cập nhật tài khoản";
            }
            return switch (httpMethod == null ? "" : httpMethod) {
                case "POST" -> "Thêm tài khoản mới";
                case "DELETE" -> "Xóa tài khoản";
                default -> "Cập nhật tài khoản";
            };
        }
        String name = switch (section) {
            case "products" -> "sản phẩm";
            case "categories" -> "danh mục";
            case "vouchers" -> "voucher";
            case "flash-sales" -> "chương trình Flash Sale";
            case "reviews" -> "đánh giá";
            case "skin-quiz" -> "trắc nghiệm da";
            case "articles", "blog" -> "bài viết";
            case "notifications" -> "thông báo";
            case "banners" -> "banner";
            default -> "";
        };
        if (name.isEmpty()) {
            return "Thay đổi dữ liệu";
        }
        return switch (httpMethod == null ? "" : httpMethod) {
            case "POST" -> "Thêm " + name + " mới";
            case "PUT", "PATCH" -> "Cập nhật " + name;
            case "DELETE" -> "Xóa " + name;
            default -> "Thay đổi " + name;
        };
    }
}

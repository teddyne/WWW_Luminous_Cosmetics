package com.luminous.controller.shop;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Cac trang "cho" o giai doan 1: link tren header/footer khong bi 404,
 * noi dung noi ro rang day la buoc tiep theo chu khong gia vo la chuc nang that.
 * Khi lam that tung chuc nang, xoa route tuong ung o day va thay bang controller that.
 */
@Controller
public class PlaceholderController {

    @GetMapping("/cart")
    public String cart(Model model) {
        model.addAttribute("title", "Giỏ hàng");
        model.addAttribute("message", "Giỏ hàng (session cho Guest, DB Cart/CartItem cho Customer) sẽ được làm ở bước tiếp theo, sau khi trang chủ và đăng nhập đã chạy ổn định.");
        return "placeholder/coming-soon";
    }

    @GetMapping("/blog")
    public String blog() {
        return "blog/blog";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("title", "Quên mật khẩu");
        model.addAttribute("message", "Chức năng đặt lại mật khẩu qua email sẽ được làm cùng bước gửi mail (cần SMTP thật). Tạm thời liên hệ admin@luminous.vn để được hỗ trợ.");
        return "placeholder/coming-soon";
    }
}

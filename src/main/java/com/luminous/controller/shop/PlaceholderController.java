package com.luminous.controller.shop;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PlaceholderController {

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("title", "Quên mật khẩu");
        model.addAttribute("message", "Chức năng đặt lại mật khẩu qua email sẽ được làm cùng bước gửi mail (cần SMTP thật). Tạm thời liên hệ admin@luminous.vn để được hỗ trợ.");
        return "placeholder/coming-soon";
    }
}

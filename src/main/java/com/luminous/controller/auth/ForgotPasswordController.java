package com.luminous.controller.auth;

import com.luminous.service.auth.AuthService;
import com.luminous.service.auth.VerificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Quen mat khau, 2 buoc trong 1 modal (giong hinh mau):
 * Buoc 1 — nhap email/sdt + captcha, bam Gui de nhan ma 6 so.
 * Buoc 2 — nhap ma 6 so + mat khau moi (nhap lai) roi dat lai.
 * Chi tai khoan da dang ky moi nhan duoc ma; ma het han sau 5 phut.
 */
@Controller
public class ForgotPasswordController {

    private final AuthService authService;

    public ForgotPasswordController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        if (!model.containsAttribute("step")) {
            model.addAttribute("step", 1);
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendCode(@RequestParam String identifier,
                           @RequestParam String captcha,
                           HttpSession session,
                           Model model) {
        String expected = (String) session.getAttribute("CAPTCHA_CODE");
        session.removeAttribute("CAPTCHA_CODE");
        model.addAttribute("step", 1);
        model.addAttribute("identifier", identifier);
        if (expected == null || !expected.equalsIgnoreCase(captcha == null ? "" : captcha.trim())) {
            model.addAttribute("error", "Mã captcha không đúng, vui lòng nhập lại");
            return "auth/forgot-password";
        }
        try {
            VerificationService.SendResult result = authService.requestResetCode(identifier);
            if (!result.ok()) {
                model.addAttribute("error", result.message());
                return "auth/forgot-password";
            }
            model.addAttribute("step", 2);
            model.addAttribute("message", result.message());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password/resend")
    public String resend(@RequestParam String identifier, Model model) {
        model.addAttribute("step", 2);
        model.addAttribute("identifier", identifier);
        try {
            VerificationService.SendResult result = authService.requestResetCode(identifier);
            model.addAttribute(result.ok() ? "message" : "error", result.message());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password/reset")
    public String reset(@RequestParam String identifier,
                        @RequestParam String code,
                        @RequestParam String newPassword,
                        @RequestParam String confirmPassword,
                        Model model,
                        RedirectAttributes redirect) {
        model.addAttribute("step", 2);
        model.addAttribute("identifier", identifier);
        if (newPassword == null || newPassword.length() < 8) {
            model.addAttribute("error", "Mật khẩu mới phải có ít nhất 8 ký tự");
            return "auth/forgot-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu nhập lại không khớp");
            return "auth/forgot-password";
        }
        try {
            authService.resetPassword(identifier, code, newPassword);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/forgot-password";
        }
        redirect.addFlashAttribute("reset", true);
        return "redirect:/login";
    }
}

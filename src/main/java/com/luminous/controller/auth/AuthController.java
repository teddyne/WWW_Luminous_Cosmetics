package com.luminous.controller.auth;

import com.luminous.dto.RegisterForm;
import com.luminous.service.auth.AuthService;
import com.luminous.service.auth.VerificationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Dang nhap: Spring Security xu ly POST /login (xem SecurityConfig),
 * controller chi hien form. Dang ky: 2 buoc — bam "Gửi mã xác thực" de
 * nhan ma 6 so qua email/SMS, nhap ma vao form roi moi bam Dang ky.
 * AuthService doi ma dung moi tao tai khoan.
 */
@Controller
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /**
     * Gui ma xac thuc dang ky (goi bang fetch tu trang dang ky).
     * Kiem tra dinh dang + trung tai khoan truoc khi gui de khong gui
     * ma toi dia chi sai hoac da co tai khoan.
     */
    @PostMapping("/register/send-code")
    @ResponseBody
    public Map<String, Object> sendRegisterCode(@RequestParam String identifier) {
        Map<String, Object> body = new HashMap<>();
        try {
            VerificationService.SendResult result = authService.requestRegisterCode(identifier);
            body.put("ok", result.ok());
            if (result.ok()) {
                body.put("message", result.message());
            } else {
                body.put("error", result.message());
            }
        } catch (IllegalArgumentException e) {
            body.put("ok", false);
            body.put("error", e.getMessage());
        }
        return body;
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult result,
            HttpSession session,
            Model model,
            RedirectAttributes redirect) {

        // Captcha: ma luu trong session khi tai anh /captcha.png
        String expected = (String) session.getAttribute("CAPTCHA_CODE");
        String captchaInput = form.getCaptcha() == null ? "" : form.getCaptcha().trim();
        if (expected == null || !expected.equalsIgnoreCase(captchaInput)) {
            result.rejectValue("captcha", "captcha.invalid", "Mã captcha không đúng, vui lòng nhập lại");
        }
        // Doi ma ngay sau khi dung (dung hay sai) de khong tai su dung lai ma cu.
        session.removeAttribute("CAPTCHA_CODE");

        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.register(form);
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("registerError", e.getMessage());
            return "auth/register";
        }
        redirect.addFlashAttribute("registered", true);
        return "redirect:/login";
    }
}

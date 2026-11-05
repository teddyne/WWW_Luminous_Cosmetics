package com.luminous.controller.auth;

import com.luminous.service.auth.CaptchaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.io.IOException;

/** GET /captcha.png: moi lan goi la 1 ma moi, luu vao session de doi chieu khi dang ky. */
@Controller
public class CaptchaController {
    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping(value = "/captcha.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> captcha(HttpSession session) throws IOException {
        String code = captchaService.generateCode();
        session.setAttribute("CAPTCHA_CODE", code);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG)
                .body(captchaService.renderImage(code));
    }
}

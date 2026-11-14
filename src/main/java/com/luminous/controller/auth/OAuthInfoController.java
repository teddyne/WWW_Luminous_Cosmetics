package com.luminous.controller.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Trang giai thich khi nut Google duoc bam nhung OAuth2 chua cau hinh.
 * Nut o trang dang nhap / dang ky luon hien; khi chua co client-id/secret thi
 * dan ve day thay vi de link chet (xem HUONG_DAN muc 4b de bat len).
 */
@Controller
@RequestMapping("/auth")
public class OAuthInfoController {

    @GetMapping("/oauth-chua-cau-hinh")
    public String oauthNotConfigured() {
        return "auth/oauth-not-configured";
    }
}

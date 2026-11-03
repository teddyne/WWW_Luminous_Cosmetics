package com.luminous.config;

import com.luminous.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;

/**
 * Phan quyen ro rang, dung theo yeu cau 3 loai nguoi dung:
 * - GUEST (chua dang nhap): xem trang chu, tim kiem, dang nhap, dang ky.
 * - CUSTOMER: them quyen gio hang, don hang, tai khoan (giai doan sau).
 * - ADMIN: toan bo quyen CUSTOMER + /admin/**.
 * Luu y: hasRole("ADMIN") tuong ung roleName ROLE_ADMIN trong DB.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Hash mau trong SQL la $2a$12$... -> BCrypt, khop voi encoder nay.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/menu", "/login", "/register", "/captcha.png", "/blog",
                                "/forgot-password", "/error", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/products/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/cart", "/cart/**", "/orders", "/orders/**",
                                "/account", "/account/**", "/checkout", "/checkout/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username") // o nhap "email hoac so dien thoai"
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler())
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // "Luu mat khau" tren form Dang nhap: checkbox name="remember-me", giu 7 ngay.
                .rememberMe(remember -> remember
                        .key("luminous-remember-me")
                        .tokenValiditySeconds(7 * 24 * 3600)
                );
        return http.build();
    }

    /**
     * Sau khi dang nhap: neu truoc do bi chan o 1 trang can quyen (vi du /cart)
     * thi quay lai trang do; neu khong thi ADMIN -> /admin, CUSTOMER -> /.
     */
    private AuthenticationSuccessHandler loginSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication auth) -> {
            HttpSessionRequestCache cache = new HttpSessionRequestCache();
            SavedRequest saved = cache.getRequest(request, response);
            if (saved != null) {
                cache.removeRequest(request, response);
                response.sendRedirect(saved.getRedirectUrl());
                return;
            }
            boolean admin = auth.getPrincipal() instanceof CustomUserDetails details && details.isAdmin();
            response.sendRedirect(admin ? "/admin" : "/");
        };
    }
}

package com.luminous.config;

import com.luminous.model.User;
import com.luminous.repository.SkinRoutineRepository;
import com.luminous.repository.UserRepository;
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
 * MOI: /skin-quiz/** va /my-routine/** chi danh cho CUSTOMER + ADMIN,
 * giong /cart, /orders, /account. GUEST bam vao se duoc dua ve /login
 * roi quay lai dung trang do (saved request).
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
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           UserRepository userRepository,
                                           SkinRoutineRepository routines) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/menu", "/login", "/register", "/captcha.png", "/blog",
                        "/forgot-password", "/error", "/favicon.ico",
                        "/css/**", "/js/**", "/images/**", "/products/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/cart", "/cart/**", "/orders", "/orders/**",
                        "/account", "/account/**", "/checkout", "/checkout/**",
                        "/skin-quiz", "/skin-quiz/**", "/my-routine", "/my-routine/**")
                    .hasAnyRole("CUSTOMER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username") // o nhap "email hoac so dien thoai"
                .passwordParameter("password")
                .successHandler(loginSuccessHandler(userRepository, routines))
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
     * Sau khi dang nhap:
     * - Neu truoc do bi chan o 1 trang can quyen (vi du /cart) thi quay lai trang do.
     * - ADMIN -> /admin.
     * - CUSTOMER lan dau (chua duoc hoi trac nghiem, chua co chu trinh)
     *   -> /skin-quiz/welcome de hoi "co muon lam trac nghiem da khong?".
     *   Co o day KHONG danh dau asked ngay: neu user tat trinh duyet o trang
     *   welcome ma chua chon gi, lan dang nhap sau van hoi lai 1 lan nua.
     *   Chi khi chon "De sau" hoac lam xong trac nghiem thi asked moi = true
     *   (xem SkinQuizController), cac lan sau khong hoi nua.
     * - CUSTOMER con lai -> /.
     * Tai khoan mau cu (admin, thanhtruc...) chua co asked=true nen cung
     * se duoc hoi dung 1 lan o lan dang nhap ke tiep sau khi chay migration 002.
     */
    private AuthenticationSuccessHandler loginSuccessHandler(
            UserRepository userRepository, SkinRoutineRepository routines) {
        return (HttpServletRequest request, HttpServletResponse response, Authentication auth) -> {
            HttpSessionRequestCache cache = new HttpSessionRequestCache();
            SavedRequest saved = cache.getRequest(request, response);
            if (saved != null) {
                cache.removeRequest(request, response);
                response.sendRedirect(saved.getRedirectUrl());
                return;
            }
            boolean admin = auth.getPrincipal() instanceof CustomUserDetails details && details.isAdmin();
            if (admin) {
                response.sendRedirect("/admin");
                return;
            }
            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null
                    && !Boolean.TRUE.equals(user.getSkinQuizAsked())
                    && routines.findByUserId(user.getId()).isEmpty()) {
                response.sendRedirect("/skin-quiz/welcome");
                return;
            }
            response.sendRedirect("/");
        };
    }
}

package com.luminous.config;

import com.luminous.model.User;
import com.luminous.repository.UserRepository;
import com.luminous.repository.skinquiz.SkinRoutineRepository;
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
                        "/", "/menu", "/login", "/register", "/captcha.png", "/blog", "/blog/**",
                        "/about", "/contact", "/support",
                        "/forgot-password", "/error", "/favicon.ico",
                        "/css/**", "/js/**", "/images/**", "/products/**",
                        "/skin-quiz", "/skin-quiz/submit", "/my-routine", "/my-routine/building",
                        "/my-routine/buy-all",
                        "/cart", "/cart/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/orders", "/orders/**",
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
     * - ADMIN -> /admin.
     * - Khach vua lam trac nghiem xong roi dang nhap de luu ket qua: dua thang
     *   ve /my-routine de hien ket qua vua lam. Buoc nay PHAI dung truoc
     *   saved request: neu truoc do nguoi dung bam nham vao 1 trang can quyen
     *   (vi du /account) thi yeu cau do da luu san se dua ho di cho khac
     *   (vi du trang chao hoi /skin-quiz/welcome) thay vi thay ket qua vua lam.
     * - Neu truoc do bi chan o 1 trang can quyen (vi du /cart) thi quay lai trang do.
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
            boolean admin = auth.getPrincipal() instanceof CustomUserDetails details && details.isAdmin();
            if (admin) {
                response.sendRedirect("/admin");
                return;
            }
            // Khach vua lam trac nghiem xong roi dang nhap de luu ket qua:
            // dua thang ve /my-routine, ket qua tam trong session se duoc
            // chuyen xuong DB o do (xem SkinQuizController).
            if (request.getSession(false) != null
                    && request.getSession(false).getAttribute(
                            com.luminous.dto.GuestRoutineSession.SESSION_KEY) != null) {
                response.sendRedirect("/my-routine");
                return;
            }
            HttpSessionRequestCache cache = new HttpSessionRequestCache();
            SavedRequest saved = cache.getRequest(request, response);
            if (saved != null) {
                cache.removeRequest(request, response);
                response.sendRedirect(saved.getRedirectUrl());
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

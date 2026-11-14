package com.luminous.config;

import com.luminous.model.User;
import com.luminous.repository.UserRepository;
import com.luminous.repository.skinquiz.SkinRoutineRepository;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.auth.CustomOAuth2UserService;
import com.luminous.service.auth.CustomOidcUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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
                                           SkinRoutineRepository routines,
                                           CustomOAuth2UserService oauth2UserService,
                                           CustomOidcUserService oidcUserService,
                                           ObjectProvider<ClientRegistrationRepository> clientRegistrations) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/menu", "/login", "/register", "/register/send-code",
                        "/captcha.png", "/blog", "/blog/**",
                        "/about", "/contact", "/support",
                        "/forgot-password", "/forgot-password/**", "/error", "/favicon.ico",
                        "/oauth2/**", "/login/oauth2/**",
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
                // Luon ghi nho phien dang nhap (ca form lan Google): cookie
                // remember-me song 7 ngay ke ca khi tat/mo lai app, vi
                // session trong bo nho cua Tomcat mat het khi app dung.
                // Dang xuat van xoa cookie nay.
                .alwaysRemember(true)
            );
        // Dang nhap bang Google: dung chung successHandler voi dang nhap
        // thuong (hoi trac nghiem lan dau, quay lai trang vua bi chan, xem
        // chu trinh vua lam luc con la khach...).
        // PHAI gan ca 2 service: Google tra ve ID token (scope openid) nen
        // Spring chay qua OIDC (.oidcUserService); chi gan .userService()
        // thi CustomOAuth2UserService khong bao gio duoc goi, principal se
        // la DefaultOidcUser (khong phai CustomUserDetails, khong co
        // ROLE_CUSTOMER) -> chon xong tai khoan Google, ve trang chu van
        // hien "chua dang nhap", /account bi chan 403.
        // CHI bat khi da cau hinh it nhat 1 provider trong properties:
        // Spring Boot chi tao bean ClientRegistrationRepository khi co
        // registration; goi .oauth2Login() luc chua co bean se lam app
        // khong khoi dong duoc ("required a bean of type
        // 'ClientRegistrationRepository' that could not be found").
        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService)
                    .oidcUserService(oidcUserService))
                .successHandler(loginSuccessHandler(userRepository, routines))
                .failureUrl("/login?error=oauth")
            );
        }
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
            String email = auth.getPrincipal() instanceof CustomUserDetails details
                    ? details.getUser().getEmail()
                    : auth.getName(); // OAuth2: getName() la sub/id, khong phai email
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

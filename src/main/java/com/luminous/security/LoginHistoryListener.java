package com.luminous.security;

import com.luminous.model.User;
import com.luminous.repository.UserRepository;
import com.luminous.service.admin.LoginHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Optional;

@Component
public class LoginHistoryListener {

    private final LoginHistoryService historyService;
    private final UserRepository users;

    public LoginHistoryListener(LoginHistoryService historyService, UserRepository users) {
        this.historyService = historyService;
        this.users = users;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        String email = emailOf(auth);
        Optional<User> user = email == null ? Optional.empty() : users.findByEmail(email);
        if (user.isEmpty() || user.get().getRole() == null || !user.get().getRole().isAdmin()) {
            return;
        }
        historyService.record(user.get().getId(), email, methodOf(auth), true, null,
                ipOf(auth), userAgent());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        Authentication auth = event.getAuthentication();
        String username = auth == null ? null : auth.getName();
        Optional<User> user = username == null ? Optional.empty() : users.findByEmail(username);
        if (user.isPresent() && (user.get().getRole() == null || !user.get().getRole().isAdmin())) {
            return;
        }
        String reason = event.getException() == null
                ? "Đăng nhập thất bại"
                : event.getException().getClass().getSimpleName()
                        + (event.getException().getMessage() == null
                                ? "" : ": " + event.getException().getMessage());
        historyService.record(user.map(User::getId).orElse(null), username,
                auth == null ? "FORM" : methodOf(auth), false, reason,
                auth == null ? null : ipOf(auth), userAgent());
    }

    private String methodOf(Authentication auth) {
        if (auth instanceof RememberMeAuthenticationToken) {
            return "REMEMBER_ME";
        }
        if (auth instanceof OAuth2AuthenticationToken) {
            return "GOOGLE";
        }
        if (auth instanceof UsernamePasswordAuthenticationToken) {
            return "FORM";
        }
        return "OTHER";
    }

    private String emailOf(Authentication auth) {
        if (auth == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails details) {
            return details.getUser().getEmail();
        }
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getAttribute("email");
        }
        return auth.getName();
    }

    private String ipOf(Authentication auth) {
        if (auth != null && auth.getDetails() instanceof WebAuthenticationDetails details) {
            return details.getRemoteAddress();
        }
        return null;
    }

    private String userAgent() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader("User-Agent");
        }
        return null;
    }
}

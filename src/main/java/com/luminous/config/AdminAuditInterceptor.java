package com.luminous.config;

import com.luminous.security.CustomUserDetails;
import com.luminous.service.admin.AdminAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuditInterceptor implements HandlerInterceptor {

    private final AdminAuditService auditService;

    public AdminAuditInterceptor(AdminAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/admin")) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
            return;
        }
        Integer userId = null;
        String username = auth.getName();
        if (auth.getPrincipal() instanceof CustomUserDetails details) {
            userId = details.getUser().getId();
            username = details.getUser().getEmail();
        }
        String handlerName = handler instanceof HandlerMethod hm
                ? hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName()
                : handler.getClass().getSimpleName();
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        auditService.record(userId, username, method, path, handlerName,
                response.getStatus(), ip);
    }
}

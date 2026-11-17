package com.luminous.service.admin;

import com.luminous.model.AdminLoginHistory;
import com.luminous.repository.AdminLoginHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginHistoryService {

    private static final Logger log = LoggerFactory.getLogger(LoginHistoryService.class);

    private final AdminLoginHistoryRepository repository;

    public LoginHistoryService(AdminLoginHistoryRepository repository) {
        this.repository = repository;
    }

    /** Ghi 1 lan dang nhap. Khong bao gio lam hong luong dang nhap neu ghi loi. */
    @Transactional
    public void record(Integer userId, String username, String method, boolean success,
                       String reason, String ipAddress, String userAgent) {
        try {
            String safeUsername = username == null ? "" : username;
            if (safeUsername.length() > 200) {
                safeUsername = safeUsername.substring(0, 200);
            }
            String safeReason = reason == null ? null
                    : reason.length() > 200 ? reason.substring(0, 200) : reason;
            String safeAgent = userAgent == null ? null
                    : userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
            repository.save(new AdminLoginHistory(userId, safeUsername, method, success,
                    safeReason, ipAddress, safeAgent));
        } catch (Exception e) {
            log.warn("Khong ghi duoc lich su dang nhap: {}", e.getMessage());
        }
    }
}

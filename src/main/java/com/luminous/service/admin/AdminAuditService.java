package com.luminous.service.admin;

import com.luminous.model.AdminAuditLog;
import com.luminous.repository.AdminAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);

    private final AdminAuditLogRepository repository;

    public AdminAuditService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    /** Ghi 1 thao tac cua admin. Khong bao gio lam hong request neu ghi loi. */
    @Transactional
    public void record(Integer userId, String username, String httpMethod, String path,
                       String handler, Integer status, String ipAddress) {
        try {
            String safePath = path == null ? "" : path;
            if (safePath.length() > 500) {
                safePath = safePath.substring(0, 500);
            }
            repository.save(new AdminAuditLog(userId, username, httpMethod, safePath,
                    handler, status, ipAddress));
        } catch (Exception e) {
            log.warn("Khong ghi duoc nhat ky thao tac admin: {}", e.getMessage());
        }
    }
}

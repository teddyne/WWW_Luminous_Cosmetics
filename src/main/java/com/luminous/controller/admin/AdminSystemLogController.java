package com.luminous.controller.admin;

import com.luminous.model.AdminAuditLog;
import com.luminous.model.AdminLoginHistory;
import com.luminous.repository.AdminAuditLogRepository;
import com.luminous.repository.AdminLoginHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminSystemLogController {

    private final AdminLoginHistoryRepository logins;
    private final AdminAuditLogRepository audits;

    public AdminSystemLogController(AdminLoginHistoryRepository logins,
                                    AdminAuditLogRepository audits) {
        this.logins = logins;
        this.audits = audits;
    }

    @GetMapping("/login-history")
    public String loginHistory(@RequestParam(required = false) String username,
                               @RequestParam(required = false) Boolean success,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        size = allowedSize(size);
        Page<AdminLoginHistory> result = logins.search(blankToNull(username), success,
                startOfDay(from), endOfDay(to), PageRequest.of(Math.max(page, 0), size));
        model.addAttribute("page", result);
        model.addAttribute("items", result.getContent());
        model.addAttribute("username", username);
        model.addAttribute("success", success);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("baseUrl", "/admin/login-history");
        model.addAttribute("pageQuery", AdminPageLinks.query(
                "username", username, "success", success, "from", from, "to", to));
        model.addAttribute("activeMenu", "admins");
        return "admin/login-history";
    }

    @GetMapping("/system-logs")
    public String systemLogs(@RequestParam(required = false) String username,
                             @RequestParam(required = false) String action,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        size = allowedSize(size);
        List<String> methods = switch (action == null ? "" : action) {
            case "CREATE" -> List.of("POST");
            case "UPDATE" -> List.of("PUT", "PATCH");
            case "DELETE" -> List.of("DELETE");
            default -> null;
        };
        Page<AdminAuditLog> result = audits.search(blankToNull(username), methods,
                startOfDay(from), endOfDay(to),
                PageRequest.of(Math.max(page, 0), size));
        model.addAttribute("page", result);
        model.addAttribute("items", result.getContent());
        model.addAttribute("username", username);
        model.addAttribute("action", action);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("baseUrl", "/admin/system-logs");
        model.addAttribute("pageQuery", AdminPageLinks.query(
                "username", username, "action", action, "from", from, "to", to));
        model.addAttribute("activeMenu", "system-logs");
        return "admin/system-logs";
    }

    private int allowedSize(int size) {
        return size == 10 || size == 20 || size == 100 ? size : 10;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59);
    }
}

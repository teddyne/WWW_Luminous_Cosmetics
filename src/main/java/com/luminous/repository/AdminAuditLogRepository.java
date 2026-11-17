package com.luminous.repository;

import com.luminous.model.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    @Query("SELECT a FROM AdminAuditLog a WHERE "
            + "(:username IS NULL OR :username = '' OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND "
            + "(:methods IS NULL OR a.httpMethod IN :methods) AND "
            + "(:from IS NULL OR a.createdAt >= :from) AND "
            + "(:to IS NULL OR a.createdAt <= :to) "
            + "ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> search(@Param("username") String username,
                               @Param("methods") List<String> methods,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               Pageable pageable);
}

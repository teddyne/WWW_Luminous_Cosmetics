package com.luminous.repository;

import com.luminous.model.AdminLoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface AdminLoginHistoryRepository extends JpaRepository<AdminLoginHistory, Long> {

    @Query("SELECT h FROM AdminLoginHistory h WHERE "
            + "(:username IS NULL OR :username = '' OR LOWER(h.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND "
            + "(:success IS NULL OR h.success = :success) AND "
            + "(:from IS NULL OR h.loginAt >= :from) AND "
            + "(:to IS NULL OR h.loginAt <= :to) "
            + "ORDER BY h.loginAt DESC")
    Page<AdminLoginHistory> search(@Param("username") String username,
                                   @Param("success") Boolean success,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   Pageable pageable);
}

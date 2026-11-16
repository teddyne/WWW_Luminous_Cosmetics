package com.luminous.repository;

import com.luminous.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    // Tru diem nguyen tu: chi tru khi du diem; tra ve 0 dong = khong du diem.
    @Modifying
    @Query("UPDATE User u SET u.pointBalance = u.pointBalance - :points "
            + "WHERE u.id = :id AND u.pointBalance >= :points")
    int deductPoints(@Param("id") Integer id, @Param("points") int points);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    long countByCreatedAtAfter(LocalDateTime after);

    long countByRole_Name(String roleName);

}

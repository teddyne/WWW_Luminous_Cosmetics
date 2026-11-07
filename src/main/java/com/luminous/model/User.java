package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`User`")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userID")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "roleID", nullable = false)
    private Role role;

    @Column(name = "fullName", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /** Luon luu dang BCrypt, KHONG bao gio luu mat khau tho, KHONG hien ra template. */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "phone", unique = true, length = 15)
    private String phone;

    @Column(name = "avtUrl", length = 500)
    private String avtUrl;

    @Column(name = "pointBalance", nullable = false)
    private Integer pointBalance = 0;

    @Column(name = "isActive", nullable = false)
    private Boolean isActive = true;

    @Column(name = "dateOfBirth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender; // NAM / NU

    @Column(name = "skinQuizAsked", nullable = false)
    private Boolean skinQuizAsked = false;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    public String roleName() {
        return role != null ? role.getName() : null;
    }
}

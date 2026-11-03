package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Khop bang Role trong luminous_db.sql: (1, ROLE_ADMIN), (2, ROLE_CUSTOMER). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roleID")
    private Integer id;

    @Column(name = "roleName", nullable = false, unique = true, length = 50)
    private String name; // ROLE_ADMIN / ROLE_CUSTOMER

    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(name);
    }
}

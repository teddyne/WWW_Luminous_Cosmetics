package com.luminous.security;

import com.luminous.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Boc User thanh UserDetails cho Spring Security. Template KHONG bao gio in password. */
@Getter
public class CustomUserDetails implements UserDetails {
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public String fullName() {
        return user.getFullName();
    }

    public String roleName() {
        return user.roleName();
    }

    public boolean isAdmin() {
        return user.getRole() != null && user.getRole().isAdmin();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = user.roleName();
        return role != null ? List.of(new SimpleGrantedAuthority(role)) : List.of();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /** Dang nhap bang email HOAC phone deu ve cung 1 principal (username noi bo = email). */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getIsActive());
    }
}

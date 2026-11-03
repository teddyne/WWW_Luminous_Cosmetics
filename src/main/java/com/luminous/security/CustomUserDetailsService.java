package com.luminous.security;

import com.luminous.model.User;
import com.luminous.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** O dang nhap nhap "email hoac so dien thoai" -> tim theo email truoc, roi den phone. */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String key = identifier == null ? "" : identifier.trim();
        User user = users.findByEmail(key)
                .orElseGet(() -> users.findByPhone(key).orElse(null));
        if (user == null) {
            throw new UsernameNotFoundException("Không tìm thấy tài khoản: " + key);
        }
        return new CustomUserDetails(user);
    }
}

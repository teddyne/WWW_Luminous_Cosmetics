package com.luminous.service.auth;

import com.luminous.dto.RegisterForm;
import com.luminous.model.Role;
import com.luminous.model.User;
import com.luminous.repository.RoleRepository;
import com.luminous.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DateTimeException;
import java.time.LocalDate;

@Service
public class AuthService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder passwords) {
        this.users = users;
        this.roles = roles;
        this.passwords = passwords;
    }

    @Transactional
    public User register(RegisterForm form) {
        String identifier = form.getIdentifier().trim();
        boolean isEmail = identifier.contains("@");
        String email;
        String phone = null;

        if (isEmail) {
            if (!identifier.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new IllegalArgumentException("Email không hợp lệ");
            }
            if (users.existsByEmail(identifier)) {
                throw new IllegalArgumentException("Email này đã được đăng ký");
            }
            email = identifier;
        } else {

            if (!identifier.matches("^0\\d{9}$")) {
                throw new IllegalArgumentException("Số điện thoại phải bắt đầu bằng 0 và đủ 10 số");
            }
            if (users.existsByPhone(identifier)) {
                throw new IllegalArgumentException("Số điện thoại này đã được đăng ký");
            }
            phone = identifier;
            email = "phone_" + identifier + "@luminous.vn";
            if (users.existsByEmail(email)) {
                throw new IllegalArgumentException("Số điện thoại này đã được đăng ký");
            }
        }

        LocalDate dob;
        try {
            dob = LocalDate.of(
                    Integer.parseInt(form.getBirthYear()),
                    Integer.parseInt(form.getBirthMonth()),
                    Integer.parseInt(form.getBirthDay()));
        } catch (DateTimeException | NumberFormatException e) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ (ví dụ: tháng 2 không có ngày 30)");
        }
        if (dob.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không được ở tương lai");
        }

        Role customer = roles.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new IllegalStateException(
                        "Chưa có role ROLE_CUSTOMER."));

        User user = new User();
        user.setRole(customer);
        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwords.encode(form.getPassword()));
        user.setDateOfBirth(dob);
        user.setGender(form.getGender());
        user.setPointBalance(0);
        user.setIsActive(true);
        return users.save(user);
    }
}

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
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;
    private final VerificationService verification;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder passwords,
                       VerificationService verification) {
        this.users = users;
        this.roles = roles;
        this.passwords = passwords;
        this.verification = verification;
    }

    /** Gui ma xac thuc dang ky: chi gui khi dinh danh dung dinh dang va chua co tai khoan. */
    public VerificationService.SendResult requestRegisterCode(String rawIdentifier) {
        String identifier = VerificationService.normalize(rawIdentifier);
        checkIdentifierFormat(identifier);
        if (VerificationService.isEmail(identifier) ? users.existsByEmail(identifier)
                : users.existsByPhone(identifier)) {
            throw new IllegalArgumentException(VerificationService.isEmail(identifier)
                    ? "Email này đã được đăng ký"
                    : "Số điện thoại này đã được đăng ký");
        }
        return verification.sendCode(identifier, VerificationService.Purpose.REGISTER);
    }

    /** Gui ma dat lai mat khau: chi gui khi tai khoan da ton tai. */
    public VerificationService.SendResult requestResetCode(String rawIdentifier) {
        String identifier = VerificationService.normalize(rawIdentifier);
        checkIdentifierFormat(identifier);
        if (findByIdentifier(identifier) == null) {
            throw new IllegalArgumentException("Email/số điện thoại này chưa đăng ký tài khoản");
        }
        return verification.sendCode(identifier, VerificationService.Purpose.RESET_PASSWORD);
    }

    private void checkIdentifierFormat(String identifier) {
        if (VerificationService.isEmail(identifier)) {
            if (!identifier.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new IllegalArgumentException("Email không hợp lệ");
            }
        } else if (!identifier.matches("^0\\d{9}$")) {
            throw new IllegalArgumentException("Số điện thoại phải bắt đầu bằng 0 và đủ 10 số");
        }
    }

    private User findByIdentifier(String identifier) {
        return VerificationService.isEmail(identifier)
                ? users.findByEmail(identifier).orElse(null)
                : users.findByPhone(identifier).orElse(null);
    }

    /** Dat lai mat khau: doi ma OTP dung (ma het han hoac sai qua 5 lan thi that bai). */
    @Transactional
    public void resetPassword(String rawIdentifier, String code, String newPassword) {
        String identifier = VerificationService.normalize(rawIdentifier);
        if (!verification.verify(identifier, code, VerificationService.Purpose.RESET_PASSWORD)) {
            throw new IllegalArgumentException("Mã xác thực không đúng hoặc đã hết hạn, vui lòng gửi lại mã mới");
        }
        User user = findByIdentifier(identifier);
        if (user == null) {
            throw new IllegalArgumentException("Email/số điện thoại này chưa đăng ký tài khoản");
        }
        user.setPassword(passwords.encode(newPassword));
        users.save(user);
    }

    /**
     * Dang nhap Google: tim theo email, co roi thi lien ket tai khoan
     * cu (khong tao trung), chua co thi tao tai khoan CUSTOMER moi.
     */
    @Transactional
    public User findOrCreateOAuthUser(String provider, String providerId,
                                     String rawEmail, String name, String pictureUrl) {
        String email = VerificationService.normalize(rawEmail);
        User user = users.findByEmail(email).orElse(null);
        if (user != null) {
            if (user.getProvider() == null || user.getProvider().isBlank()) {
                user.setProvider(provider);
            }
            if (user.getProviderId() == null && providerId != null) {
                user.setProviderId(providerId);
            }
            if ((user.getAvtUrl() == null || user.getAvtUrl().isBlank())
                    && pictureUrl != null && !pictureUrl.isBlank()) {
                user.setAvtUrl(pictureUrl);
            }
            return users.save(user);
        }
        Role customer = roles.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Chưa có role ROLE_CUSTOMER."));
        User created = new User();
        created.setRole(customer);
        String fallbackName = email.contains("@") ? email.substring(0, email.indexOf('@')) : "Khách hàng";
        created.setFullName(name == null || name.isBlank() ? fallbackName : name.trim());
        created.setEmail(email);
        created.setPhone(null);
        // Mat khau ngau nhien, khong ai biet: chi dang nhap duoc bang Google.
        created.setPassword(passwords.encode(UUID.randomUUID().toString()));
        created.setProvider(provider);
        created.setProviderId(providerId);
        created.setAvtUrl(pictureUrl);
        created.setPointBalance(0);
        created.setIsActive(true);
        return users.save(created);
    }

    @Transactional
    public User register(RegisterForm form) {
        String identifier = VerificationService.normalize(form.getIdentifier());
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

        // Doi ma SAU khi moi kiem tra khac da qua: nhap sai ngay sinh thi
        // khong bi mat ma oan. Ma dung thi tao tai khoan, ma dung 1 lan roi huy.
        if (!verification.verify(identifier, form.getVerificationCode(),
                VerificationService.Purpose.REGISTER)) {
            throw new IllegalArgumentException("Mã xác thực không đúng hoặc đã hết hạn, vui lòng gửi lại mã mới");
        }

        Role customer = roles.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new IllegalStateException(
                        "Chưa có role ROLE_CUSTOMER."));

        User user = new User();
        user.setRole(customer);
        user.setProvider("LOCAL");
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

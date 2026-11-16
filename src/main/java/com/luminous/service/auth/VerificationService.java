package com.luminous.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    public enum Purpose {
        REGISTER, RESET_PASSWORD, CHANGE_PHONE
    }

    public record SendResult(boolean ok, String channel, String message) {
    }

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;

    private record CodeRecord(String code, LocalDateTime expiresAt, LocalDateTime sentAt, int attempts) {
    }

    private final Map<String, CodeRecord> codes = new ConcurrentHashMap<>();
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String mailFrom;
    private final SecureRandom random = new SecureRandom();

    public VerificationService(
                               ObjectProvider<JavaMailSender> mailSenders,
                               @Value("${spring.mail.username:}") String mailFrom) {
        this.mailSenders = mailSenders;
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
    }

    /** Email luon luu dang chu thuong, sdt giu nguyen nhu nguoi dung nhap. */
    public static String normalize(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        return value.contains("@") ? value.toLowerCase() : value;
    }

    public static boolean isEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }

    /**
     * Sinh ma moi va gui di. Tra ve ok=false (kem ly do tieng Viet) khi
     * dang trong thoi gian cho gui lai, thay vi sinh ma moi de ma cu.
     */
    public SendResult sendCode(String identifier, Purpose purpose) {
        String key = purpose.name() + ":" + normalize(identifier);
        LocalDateTime now = LocalDateTime.now();
        CodeRecord existing = codes.get(key);
        if (existing != null && existing.sentAt().plus(RESEND_COOLDOWN).isAfter(now)
                && existing.expiresAt().isAfter(now)) {
            long wait = Duration.between(now, existing.sentAt().plus(RESEND_COOLDOWN)).getSeconds() + 1;
            return new SendResult(false, "", "Vui lòng đợi " + wait + " giây rồi gửi lại mã");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        codes.put(key, new CodeRecord(code, now.plus(CODE_TTL), now, 0));

        if (isEmail(identifier)) {
            return sendByEmail(normalize(identifier), code, purpose);
        }
        return new SendResult(false, "SMS", "Chỉ hỗ trợ gửi mã qua email");
    }

    private SendResult sendByEmail(String email, String code, Purpose purpose) {
        String subject = switch (purpose) {
            case REGISTER -> "Mã xác thực đăng ký Luminous";
            case RESET_PASSWORD -> "Mã đặt lại mật khẩu Luminous";
            case CHANGE_PHONE -> "Mã xác thực đổi số điện thoại Luminous";
        };
        String text = "Mã xác thực 6 chữ số của bạn là: " + code
                + "\nMã có hiệu lực trong 5 phút. Không chia sẻ mã này cho bất kỳ ai.";
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null || mailFrom.isBlank()) {
            // Chua cau hinh SMTP: che do demo, ma chi ghi ra log server.
            log.warn("OTP (DEMO, chua cau hinh SMTP) cho {}: {}", email, code);
            return new SendResult(true, "EMAIL",
                    "Đã tạo mã xác thực. Chưa cấu hình SMTP nên mã đang ghi trong log server"
                            + " — cấu hình spring.mail.* rồi gửi lại để nhận mã qua email thật.");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject(subject);
            message.setText(text);
            sender.send(message);
        } catch (MailException e) {
            log.warn("Gui mail OTP toi {} that bai: {}", email, e.getMessage());
            codes.remove(Purpose.REGISTER.name() + ":" + email);
            codes.remove(Purpose.RESET_PASSWORD.name() + ":" + email);
            return new SendResult(false, "EMAIL",
                    "Không gửi được email (kiểm tra lại cấu hình SMTP hoặc địa chỉ email)");
        }
        return new SendResult(true, "EMAIL", "Đã gửi mã xác thực 6 chữ số tới email của bạn (hiệu lực 5 phút)");
    }



    /**
     * Doi chieu ma. Dung thi tieu thu ma ngay (khong dung lai duoc lan 2).
     * Sai qua 5 lan hoac het han thi huy ma.
     */
    public boolean verify(String identifier, String code, Purpose purpose) {
        String key = purpose.name() + ":" + normalize(identifier);
        CodeRecord record = codes.get(key);
        if (record == null || code == null) {
            return false;
        }
        if (record.expiresAt().isBefore(LocalDateTime.now())) {
            codes.remove(key);
            return false;
        }
        if (record.attempts() >= MAX_ATTEMPTS) {
            codes.remove(key);
            return false;
        }
        if (!record.code().equals(code.trim())) {
            codes.put(key, new CodeRecord(record.code(), record.expiresAt(), record.sentAt(), record.attempts() + 1));
            return false;
        }
        codes.remove(key);
        return true;
    }
}

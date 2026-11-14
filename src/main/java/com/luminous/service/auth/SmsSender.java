package com.luminous.service.auth;

/**
 * Cong gui SMS that. Muon gui SMS that, viet mot lop implements interface
 * nay (vi du goi API Twilio, Viettel SMS, eSMS...), dang ky no thanh bean
 * Spring thi {@link VerificationService} se tu dung no thay cho ban log.
 * Chi can 1 bean SmsSender duy nhat trong ung dung.
 */
public interface SmsSender {

    /** Gui tin nhan toi so dien thoai VN dang 0xxxxxxxxx. Nem RuntimeException khi gui that bai. */
    void sendSms(String phone, String message);
}

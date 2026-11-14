package com.luminous.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Ban SMS tam cho bai tap: khong gui tin nhan that, chi ghi ma ra log
 * server de demo duoc luong xac thuc. Khi co tai khoan SMS tra phi
 * (Twilio...), thay bang bean SmsSender that thi khong can sua code khac.
 */
@Service
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void sendSms(String phone, String message) {
        log.warn("SMS (DEMO, chua co nha cung cap SMS that) toi {}: {}", phone, message);
    }
}

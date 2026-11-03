package com.luminous;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LuminousApplication {
    public static void main(String[] args) {
        // Captcha ve anh bang AWT: bat headless de chay duoc ca tren server khong co man hinh.
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(LuminousApplication.class, args);
    }
}

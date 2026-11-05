package com.luminous.service.auth;

import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * Captcha tu ve bang Java (khong can key Google reCAPTCHA):
 * ma 5 ky tu luu vao HttpSession, anh PNG nen xanh nhat theo Figma,
 * nut refresh chi can tai lai anh voi tham so ngau nhien.
 */
@Service
public class CaptchaService {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // bo 0/O/1/I de kho nham
    private final SecureRandom random = new SecureRandom();

    public String generateCode() {
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public byte[] renderImage(String code) throws IOException {
        int width = 150, height = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0xEA, 0xF3, 0xEC)); // nen xanh nhat
        g.fillRect(0, 0, width, height);
        // Vach nhieu chong bot (chi la nhieu thi giac, khong anh huong ma)
        g.setColor(new Color(0xB9, 0xD2, 0xBE));
        for (int i = 0; i < 8; i++) {
            int y = random.nextInt(height);
            g.drawLine(0, y, width, y + random.nextInt(11) - 5);
        }
        // Ve tung ky tu lech nhau mot chut
        Font[] fonts = {
                new Font("SansSerif", Font.BOLD, 26),
                new Font("Serif", Font.BOLD | Font.ITALIC, 26),
                new Font("SansSerif", Font.BOLD, 24)
        };
        Color[] colors = {new Color(0x2F, 0x6B, 0x4F), new Color(0x3A, 0x5A, 0x8A), new Color(0x6B, 0x4F, 0x2F)};
        for (int i = 0; i < code.length(); i++) {
            g.setFont(fonts[random.nextInt(fonts.length)]);
            g.setColor(colors[random.nextInt(colors.length)]);
            g.drawString(String.valueOf(code.charAt(i)), 16 + i * 26, 30 + random.nextInt(9));
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}

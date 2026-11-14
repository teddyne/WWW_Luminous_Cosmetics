package com.luminous.config;

import com.luminous.repository.UserRepository;
import com.luminous.repository.notification.NotificationRepository;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.shop.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CartService cartService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;
    private final NotificationRepository notifications;
    private final UserRepository users;

    public GlobalModelAttributes(CartService cartService,
                                 ObjectProvider<ClientRegistrationRepository> clientRegistrations,
                                 NotificationRepository notifications,
                                 UserRepository users) {
        this.cartService = cartService;
        this.clientRegistrations = clientRegistrations;
        this.notifications = notifications;
        this.users = users;
    }

    @ModelAttribute("currentUser")
    public CustomUserDetails currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            // Tai lai User tu DB moi lan render: so du diem thay doi sau khi
            // dang nhap (dung diem mua hang, duoc cong khi danh gia / hoan
            // thanh don), principal chi giu ban User luc dang nhap nen
            // header/trang Ho so se hien so cu neu khong tai lai.
            return users.findById(details.getUser().getId())
                    .map(CustomUserDetails::new)
                    .orElse(details);
        }
        return null;
    }

    @ModelAttribute("cartCount")
    public int cartCount(Authentication authentication, HttpSession session) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return cartService.countItems(details.getUser());
        }
        return cartService.countGuestItems(session);
    }

    /**
     * Nut Google o trang dang nhap/dang ky luon hien (xem SecurityConfig):
     * chua cau hinh thi bam mo trang huong dan, cau hinh xong thi dang
     * nhap that. Chi can biet da cau hinh hay chua de chon dung link.
     */
    @ModelAttribute("oauthEnabled")
    public boolean oauthEnabled() {
        return clientRegistrations.getIfAvailable() != null;
    }

    /** So thong bao chua doc (hien huy hieu o sidebar). Chua dang nhap = 0. */
    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return notifications.countByUserIdAndIsReadFalse(details.getUser().getId());
        }
        return 0;
    }
}

package com.luminous.config;

import com.luminous.security.CustomUserDetails;
import com.luminous.service.shop.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CartService cartService;

    public GlobalModelAttributes(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("currentUser")
    public CustomUserDetails currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }
        return null;
    }

    @ModelAttribute("cartCount")
    public int cartCount(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return cartService.countItems(details.getUser());
        }
        return 0;
    }
}

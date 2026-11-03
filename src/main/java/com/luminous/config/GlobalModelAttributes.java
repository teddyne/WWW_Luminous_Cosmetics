package com.luminous.config;

import com.luminous.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Dua currentUser vao moi template (de header hien ten / menu theo role)
 * ma khong can truyen tay o tung controller.
 * currentUser = null khi la GUEST.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("currentUser")
    public CustomUserDetails currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }
        return null;
    }
}

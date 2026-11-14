package com.luminous.service.auth;

import com.luminous.model.User;
import com.luminous.security.CustomUserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Boc User (da luu DB) thanh OAuth2User / OidcUser cho Spring Security, tai su
 * dung moi thu cua CustomUserDetails (quyen, isAdmin, isEnabled...) nen trang
 * ca nhan, gio hang, phan quyen chay y nhu dang nhap bang mat khau.
 * Implement ca OidcUser vi Google dang nhap qua OIDC (xem CustomOidcUserService):
 * principal phai vua la CustomUserDetails vua la OidcUser thi header moi nhan
 * ra tai khoan va /account moi co ROLE_CUSTOMER.
 */
public class CustomOAuth2User extends CustomUserDetails implements OAuth2User, OidcUser {

    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    /** Dang nhap OAuth2 thuong (khong co ID token): idToken/userInfo = null. */
    public CustomOAuth2User(User user, Map<String, Object> attributes, String nameAttributeKey) {
        this(user, attributes, nameAttributeKey, null, null);
    }

    /** Dang nhap OIDC (Google): giu lai ID token + user info tu Google. */
    public CustomOAuth2User(User user, Map<String, Object> attributes, String nameAttributeKey,
                            OidcIdToken idToken, OidcUserInfo userInfo) {
        super(user);
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return super.getAuthorities();
    }

    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    /**
     * Ten dinh danh OAuth2 (Google: sub). KHONG phai email:
     * SecurityConfig lay email qua ((CustomUserDetails) principal).getUser().getEmail().
     */
    @Override
    public String getName() {
        Object value = attributes.get(nameAttributeKey);
        return value != null ? value.toString() : getUser().getEmail();
    }
}

package com.luminous.service.auth;

import com.luminous.model.User;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Dang nhap bang Google (OIDC). Google tra ve ca ID token (scope openid)
 * nen Spring Security chay qua OidcUserService, KHONG qua OAuth2UserService
 * thuong: chi gan CustomOAuth2UserService vao .userService() thi ham
 * loadUser ben do khong bao gio duoc goi, principal se la DefaultOidcUser
 * (khong phai CustomUserDetails) -> header tuong la chua dang nhap, /account
 * bi chan 403 vi khong co ROLE_CUSTOMER. Vi vay phai co service nay va gan
 * vao .oidcUserService() trong SecurityConfig (xem ghi chu o do).
 *
 * Luong giong CustomOAuth2UserService: lay email -> tim tai khoan theo email,
 * co roi thi lien ket (luu provider/providerId), chua co thi tao tai khoan
 * CUSTOMER moi (xem AuthService.findOrCreateOAuthUser).
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private final AuthService authService;

    public CustomOidcUserService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();

        String providerId = attributeAsString(attributes, "sub");
        String email = attributeAsString(attributes, "email");
        String name = attributeAsString(attributes, "name");
        String picture = attributeAsString(attributes, "picture");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    "Tài khoản Google không chia sẻ email, không tạo được tài khoản Luminous");
        }

        User user = authService.findOrCreateOAuthUser("GOOGLE", providerId, email, name, picture);
        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        return new CustomOAuth2User(user, attributes, nameAttributeKey,
                oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private static String attributeAsString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? null : value.toString();
    }
}

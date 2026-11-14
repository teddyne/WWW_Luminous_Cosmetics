package com.luminous.service.auth;

import com.luminous.model.User;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Dang nhap bang Google (OAuth2 thuong, khong qua OIDC). Luong:
 * Google tra ve email -> tim tai khoan theo email, co roi thi lien ket
 * (luu provider/providerId), chua co thi tao tai khoan CUSTOMER moi
 * (xem AuthService.findOrCreateOAuthUser). Mat khau cua tai khoan OAuth2
 * la chuoi ngau nhien da BCrypt, khong ai biet, chi dang nhap duoc bang
 * Google hoac dat lai mat khau qua email.
 *
 * CHU Y: Google dang nhap that qua OIDC (scope openid) nen service nay
 * chi la du phong; luong chinh chay qua CustomOidcUserService (gan vao
 * .oidcUserService() trong SecurityConfig). Khong gan ca 2 thi dang nhap
 * Google xong van hien "chua dang nhap" (xem ghi chu ben do).
 *
 * Can them spring-boot-starter-oauth2-client vao pom va dien client-id /
 * client-secret trong application.properties (lay tu Google Cloud Console)
 * thi nut moi chay that.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthService authService;

    public CustomOAuth2UserService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oauthUser.getAttributes();

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
        return new CustomOAuth2User(user, attributes, nameAttributeKey);
    }

    private static String attributeAsString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? null : value.toString();
    }
}

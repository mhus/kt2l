package de.mhus.kt2l.aaa.oauth2;

import de.mhus.kt2l.aaa.AaaUser;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Map;

public interface OAuth2AuthProvider {

    String getRegistrationId();

    String getTitle();

    String getLoginUrlTemplate();

    OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes);

    String getImageResourcePath();

    boolean canHandle(DefaultOidcUser userDetails);

    AaaUser createAaaUser(DefaultOidcUser userDetails);

}

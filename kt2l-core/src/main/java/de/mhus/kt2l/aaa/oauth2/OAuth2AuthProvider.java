package de.mhus.kt2l.aaa.oauth2;

import com.vaadin.flow.component.UI;
import de.mhus.kt2l.aaa.AaaUser;
import de.mhus.kt2l.aaa.SecurityService;
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

    default void logout(AaaUser user) {
        UI.getCurrent().getPage().setLocation(SecurityService.LOGOUT_SUCCESS_URL);
    }

}

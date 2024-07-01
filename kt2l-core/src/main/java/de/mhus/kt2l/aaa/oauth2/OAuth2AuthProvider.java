package de.mhus.kt2l.aaa.oauth2;

import java.util.Map;

public interface OAuth2AuthProvider {

    String getRegistrationId();

    String getTitle();

    String getLoginUrlTemplate();

    OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes);

    String getImageResourcePath();
}

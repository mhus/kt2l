package de.mhus.kt2l.aaa.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class GoogleAuthProvider implements OAuth2AuthProvider {

    @Override
    public String getRegistrationId() {
        return "google";
    }

    @Override
    public String getTitle() {
        return "Google";
    }

    @Override
    public String getLoginUrlTemplate() {
        return "/oauth2/authorization/google";
    }

    @Override
    public OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes) {
        LOGGER.info("Google createOAuth2UserInfo with attributes: {}", attributes);
        return new GoogleOAuth2UserInfo(attributes);
    }

    @Override
    public String getImageResourcePath() {
        return "/images/google-logo.svg";
    }

    public static class GoogleOAuth2UserInfo extends OAuth2UserInfo {

        public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
            super(attributes);
        }

        @Override
        public String getId() {
            return (String) attributes.get("sub");
        }

        @Override
        public String getName() {
            return (String) attributes.get("name");
        }

        @Override
        public String getEmail() {
            return (String) attributes.get("email");
        }

        @Override
        public String getImageUrl() {
            return (String) attributes.get("picture");
        }
    }
}

package de.mhus.kt2l.aaa.oauth2;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GithubAuthProvider implements OAuth2AuthProvider {

    @Override
    public String getRegistrationId() {
        return "github";
    }

    @Override
    public String getTitle() {
        return "Github";
    }

    @Override
    public String getLoginUrlTemplate() {
        return "${baseUrl}/login/oauth2/code/github";
    }

    @Override
    public OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes) {
        return new GithubOAuth2UserInfo(attributes);
    }

    @Override
    public String getImageResourcePath() {
        return "/images/github-logo.svg";
    }

    public static class GithubOAuth2UserInfo extends OAuth2UserInfo {

        public GithubOAuth2UserInfo(Map<String, Object> attributes) {
            super(attributes);
        }

        @Override
        public String getId() {
            return ((Integer) attributes.get("id")).toString();
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
            return (String) attributes.get("avatar_url");
        }
    }
}

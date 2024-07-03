package de.mhus.kt2l.aaa.oauth2;

import de.mhus.kt2l.aaa.AaaUser;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GithubAuthProvider extends AbstractOAuth2AuthProvider {

    @Override
    public String getRegistrationId() {
        return "github";
    }

    @Override
    public String getTitle() {
        return "Github";
    }

    @Override
    public OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes) {
        return new GithubOAuth2UserInfo(attributes);
    }

    @Override
    public boolean canHandle(DefaultOidcUser userDetails) {
        return false; //XXX
    }

    @Override
    public AaaUser createAaaUser(DefaultOidcUser userDetails) {
        return null; //XXX
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

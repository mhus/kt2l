package de.mhus.kt2l.aaa.oauth2;

import de.mhus.kt2l.aaa.AaaUser;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FacebookAuthProvider extends AbstractOAuth2AuthProvider {

    @Override
    public String getRegistrationId() {
        return "facebook";
    }

    @Override
    public String getTitle() {
        return "Facebook";
    }

    @Override
    public OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes) {
        return new FacebookOAuth2UserInfo(attributes);
    }

    @Override
    public boolean canHandle(DefaultOidcUser userDetails) {
        return false; //XXX
    }

    @Override
    public AaaUser createAaaUser(DefaultOidcUser userDetails) {
        return null; //XXX
    }

    public static class FacebookOAuth2UserInfo extends OAuth2UserInfo {
        public FacebookOAuth2UserInfo(Map<String, Object> attributes) {
            super(attributes);
        }

        @Override
        public String getId() {
            return (String) attributes.get("id");
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
            if(attributes.containsKey("picture")) {
                Map<String, Object> pictureObj = (Map<String, Object>) attributes.get("picture");
                if(pictureObj.containsKey("data")) {
                    Map<String, Object>  dataObj = (Map<String, Object>) pictureObj.get("data");
                    if(dataObj.containsKey("url")) {
                        return (String) dataObj.get("url");
                    }
                }
            }
            return null;
        }
    }
}

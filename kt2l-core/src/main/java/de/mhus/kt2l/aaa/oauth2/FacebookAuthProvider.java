package de.mhus.kt2l.aaa.oauth2;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FacebookAuthProvider implements OAuth2AuthProvider {

    @Override
    public String getRegistrationId() {
        return "facebook";
    }

    @Override
    public String getTitle() {
        return "Facebook";
    }

    @Override
    public String getLoginUrlTemplate() {
        return "${baseUrl}/login/oauth2/code/facebook";
    }

    @Override
    public OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes) {
        return new FacebookOAuth2UserInfo(attributes);
    }

    @Override
    public String getImageResourcePath() {
        return "/images/facebook-logo.svg";
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

/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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

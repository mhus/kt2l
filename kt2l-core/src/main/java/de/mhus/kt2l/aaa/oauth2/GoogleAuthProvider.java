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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class GoogleAuthProvider extends AbstractOAuth2AuthProvider {

    @Override
    public String getRegistrationId() {
        return "google";
    }

    @Override
    public String getTitle() {
        return "Google";
    }

    @Override
    public OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes) {
        LOGGER.info("Google createOAuth2UserInfo with attributes: {}", attributes);
        return new GoogleOAuth2UserInfo(attributes);
    }

/*
this.user = {DefaultOidcUser@12467} "Name: [114434824555433513888], Granted Authorities: [[OIDC_USER, SCOPE_https://www.googleapis.com/auth/userinfo.email, SCOPE_https://www.googleapis.com/auth/userinfo.profile, SCOPE_openid]], User Attributes: [{at_hash=dwsxDC1GSQ2yWQxEy3EOmQ, sub=114434824555433513888, email_verified=true, iss=https://accounts.google.com, given_name=Mike, nonce=TZA-A3dRDcpdiGYaPt0noCu9CGv-ifOqj8ioOycdsV0, picture=https://lh3.googleusercontent.com/a/ACg8ocJJcjb6TPxBkOBJikqFpm2vOlWP_k66m5DLC8R3cppTgDfZRknp=s96-c, aud=[958259406990-62fqt3o82u8hct6i1cjfjcutq9cbpvsu.apps.googleusercontent.com], azp=958259406990-62fqt3o82u8hct6i1cjfjcutq9cbpvsu.apps.googleusercontent.com, name=Mike Hummel (Jesus), exp=2024-07-01T22:03:54Z, family_name=Hummel, iat=2024-07-01T21:03:54Z, email=msgformike@gmail.com}]"
 idToken = {OidcIdToken@12964}
 userInfo = null
 authorities = {Collections$UnmodifiableSet@12965}  size = 4
  0 = {OidcUserAuthority@12969} "OIDC_USER"
  1 = {SimpleGrantedAuthority@12970} "SCOPE_https://www.googleapis.com/auth/userinfo.email"
  2 = {SimpleGrantedAuthority@12971} "SCOPE_https://www.googleapis.com/auth/userinfo.profile"
  3 = {SimpleGrantedAuthority@12972} "SCOPE_openid"
 attributes = {Collections$UnmodifiableMap@12966}  size = 14
  "at_hash" -> "dwsxDC1GSQ2yWQxEy3EOmQ"
  "sub" -> "114434824555433513888"
  "email_verified" -> {Boolean@12996} true
  "iss" -> {URL@12998} "https://accounts.google.com"
  "given_name" -> "Mike"
  "nonce" -> "TZA-A3dRDcpdiGYaPt0noCu9CGv-ifOqj8ioOycdsV0"
  "picture" -> "https://lh3.googleusercontent.com/a/ACg8ocJJcjb6TPxBkOBJikqFpm2vOlWP_k66m5DLC8R3cppTgDfZRknp=s96-c"
  "aud" -> {ArrayList@13006}  size = 1
  "azp" -> "958259406990-62fqt3o82u8hct6i1cjfjcutq9cbpvsu.apps.googleusercontent.com"
  "name" -> "Mike Hummel (Jesus)"
  "exp" -> {Instant@13012} "2024-07-01T22:03:54Z"
  "family_name" -> "Hummel"
  "iat" -> {Instant@13016} "2024-07-01T21:03:54Z"
  "email" -> "msgformike@gmail.com"
 nameAttributeKey = "sub"

 */
    @Override
    public boolean canHandle(DefaultOidcUser userDetails) {
        var iss = userDetails.getAttribute("iss");
        if (iss == null)
            return false;
        return iss.toString().equals("https://accounts.google.com");
    }

    @Override
    public AaaUser createAaaUser(DefaultOidcUser userDetails) {
        LOGGER.debug("Google createAaaUser with userDetails: {}", userDetails);
        var builder = AaaUser.builder();
        builder.userId(userDetails.getAttribute("sub").toString() + "@" + getRegistrationId());
        builder.provider(getRegistrationId());
        builder.providerId(userDetails.getAttribute("sub").toString());
        builder.email(userDetails.getAttribute("email").toString());
        builder.displayName(userDetails.getAttribute("name").toString());
        builder.imageUrl(userDetails.getAttribute("picture").toString());
        builder.encodedPassword("{none}");
        builder.roles(mapRoles(getRoles(userDetails.getAuthorities())));

        return builder.build();
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

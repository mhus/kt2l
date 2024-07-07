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

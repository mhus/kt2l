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

import de.mhus.kt2l.aaa.LoginConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashSet;

public abstract class AbstractOAuth2AuthProvider implements OAuth2AuthProvider {

    protected LoginConfiguration.OAuthProvider config;

    @Autowired
    protected LoginConfiguration loginConfiguration;

    @PostConstruct
    public void init() {
        config = loginConfiguration.getOAuth2Provider(getRegistrationId()).orElse(null);
    }

    @Override
    public String getLoginUrlTemplate() {
        return "/oauth2/authorization/" + getRegistrationId();
    }

    @Override
    public String getImageResourcePath() {
        return "/images/" + getRegistrationId() + "-logo.svg";
    }


    protected Collection<String> mapRoles(Collection<String> roles) {
        var result = new HashSet<String>();
        roles.forEach(role -> result.addAll(config.getRoleMapping(role)));
        return result;
    }

    protected Collection<String> getRoles(Collection<? extends GrantedAuthority> authorities) {
        return new HashSet<>(authorities.stream().map(GrantedAuthority::getAuthority).toList());
    }


}

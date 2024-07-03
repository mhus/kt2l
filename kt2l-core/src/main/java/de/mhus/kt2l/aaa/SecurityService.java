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

package de.mhus.kt2l.aaa;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import de.mhus.kt2l.aaa.oauth2.AuthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SecurityService {

    public static final String UI_USER = "user";
    @Autowired
    private AaaConfiguration configuration;
    @Autowired
    private AuthProvider authProvider;

    public static final String LOGOUT_SUCCESS_URL = "/";

    public AaaUser getUser() {
        return de.mhus.kt2l.aaa.SecurityContext.lookupUser();
    }

    public Set<String> getRolesForResource(String resourceScope, String resourceName) {
        return configuration.getRoles(resourceScope, resourceName);
    }

    public boolean hasRole(String role) {
        return SecurityUtils.hasUserRoles(Set.of(role));
    }

    public boolean hasRole(String resourceScope, String resourceName, String ... defaultRole) {
        return hasRole(resourceScope, resourceName, Set.of(defaultRole));
    }

    public boolean hasRole(String resourceScope, String resourceName, Set<String> defaultRole) {
        Set<String> roles = getRolesForResource(resourceScope, resourceName);
        if (roles == null)
            roles = defaultRole;
        if (roles == null)
            return false;
        return SecurityUtils.hasUserRoles(roles);
    }

    public boolean hasRole(String resourceScope, String resourceName, Set<String> defaultRole, AaaUser user) {
        Set<String> roles = getRolesForResource(resourceScope, resourceName);
        if (roles == null)
            roles = defaultRole;
        if (roles == null)
            return false;
        return SecurityUtils.hasUserRoles(roles, user);
    }

    public boolean hasRole(String resourceScope, Object resource) {
        if (resource == null) return false;
        final var roles = configuration.getRoles(resourceScope, SecurityUtils.getResourceId(resource));
        if (roles != null) {
            return SecurityUtils.hasUserRoles(roles);
        }
        return SecurityUtils.hasUserResourceRoles(resource);
    }
//XXX
//    public UserDetails getAuthenticatedUser() {
//        SecurityContext context = SecurityContextHolder.getContext();
//        Object principal = context.getAuthentication().getPrincipal();
//        if (principal instanceof UserDetails) {
//            return (UserDetails) context.getAuthentication().getPrincipal();
//        }
//        // Anonymous or not authenticated.
//        return null;
//    }

    public void logout() {
        AaaUser user = getUser();
        authProvider.getProvider(user.getProvider()).ifPresentOrElse(
                p -> {
                    p.logout(user);
                },
                () -> {
                    UI.getCurrent().getPage().setLocation(LOGOUT_SUCCESS_URL);
                }
        );
        UI.getCurrent().getSession().setAttribute(UI_USER, null);
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(
                VaadinServletRequest.getCurrent().getHttpServletRequest(), null,
                null);
    }

}
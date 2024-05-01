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

package de.mhus.kt2l.core;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import de.mhus.kt2l.config.AaaConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Set;

@Component
public class SecurityService {

    @Autowired
    private AaaConfiguration configuration;

    private static final String LOGOUT_SUCCESS_URL = "/";

    public Principal getPrincipal() {
        return de.mhus.kt2l.core.SecurityContext.lookupPrincipal();
    }

    public Set<String> getRolesForResource(String resourceScope, String resourceName) {
        return configuration.getRoles(resourceScope, resourceName);
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
        return SecurityUtils.hasPrincipalRoles(roles);
    }

    public boolean hasRole(String resourceScope, String resourceName, Set<String> defaultRole, Principal principal) {
        Set<String> roles = getRolesForResource(resourceScope, resourceName);
        if (roles == null)
            roles = defaultRole;
        if (roles == null)
            return false;
        return SecurityUtils.hasPrincipalRoles(roles, principal);
    }

    public boolean hasRole(String resourceScope, Object resource) {
        if (resource == null) return false;
        final var roles = configuration.getRoles(resourceScope, SecurityUtils.getResourceId(resource));
        if (roles != null) {
            return SecurityUtils.hasPrincipalRoles(roles);
        }
        return SecurityUtils.hasPrincipalResourceRoles(resource);
    }

    public UserDetails getAuthenticatedUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        Object principal = context.getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return (UserDetails) context.getAuthentication().getPrincipal();
        }
        // Anonymous or no authentication.
        return null;
    }

    public void logout() {
        UI.getCurrent().getPage().setLocation(LOGOUT_SUCCESS_URL);
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(
                VaadinServletRequest.getCurrent().getHttpServletRequest(), null,
                null);
    }

}
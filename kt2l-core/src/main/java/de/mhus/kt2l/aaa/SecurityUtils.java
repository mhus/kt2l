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
import com.vaadin.flow.server.VaadinSession;
import de.mhus.commons.io.MHttp;
import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.util.MUri;
import de.mhus.kt2l.aaa.UsersConfiguration.ROLE;
import de.mhus.kt2l.core.ResourceId;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class SecurityUtils {

    private static final String LOGOUT_SUCCESS_URL = "/";
    private static Boolean unsecure = null;

    public static AaaUser getUser() {
        return (AaaUser)UI.getCurrent().getSession().getAttribute(SecurityService.UI_USER);
    }

    public static String getResourceId(Object resource) {
        if (resource == null) return null;
        ResourceId idAnnotation = resource.getClass().getAnnotation(ResourceId.class);
        if (idAnnotation != null)
            return idAnnotation.value();
        return MSystem.getClassName(resource);
    }

    public static boolean isUnsecure() {
        if (unsecure == null)
            unsecure = MCast.toboolean(System.getProperty("KT2L_UNSECURE"), true);
        return unsecure;
    }

    public static void exitToLogin(String errorMessage) {
        var ui = UI.getCurrent();
        ui.getSession().setAttribute("autologin", "false");
        ui.getPage().setLocation("/login?error=" + MUri.encode(errorMessage));
    }

    boolean hasUserRoles(Object resource, Set<String> roles) {
        if (resource == null || roles == null) return false;

        final var withRole = resource.getClass().getAnnotation(WithRole.class);
        if (withRole == null) return false;
        for (ROLE checkRole : withRole.value())
            if (!roles.contains(checkRole.name())) return false;
        return true;
    }

    /**
     * Check if the current user has the roles
     * @param roles
     * @return
     */
    static boolean hasUserRoles(Set<String> roles) {
        final var user = SecurityContext.lookupUser();
        if (user == null) {
            LOGGER.warn("User not found in request");
            return false;
        }
        return hasUserRoles(roles, user);
    }

    static boolean hasUserRoles(Set<String> roles, AaaUser user) {

        if (user == null) {
            LOGGER.warn("User not found", new Throwable());
            return false;
        }

        for (String resourceRole : roles) {
            if (!user.getRoles().contains(resourceRole)) return false;
        }
        return true;
    }

    // do not use
    static boolean hasUserResourceRoles(Object resource) {
        final var user = SecurityContext.lookupUser();
        if (user == null) {
            LOGGER.warn("User not found in request");
            return false;
        }

        final var withRole = resource.getClass().getAnnotation(WithRole.class);
        if (withRole == null) return false;

        for (var role : withRole.value())
            if (!user.getRoles().contains(role.name())) return false;
        return true;
    }

    // do not use
    static boolean hasUserResourceRoles(Object resource, ROLE role) {
        if (resource == null) return false;
        final var withRole = resource.getClass().getAnnotation(WithRole.class);
        if (withRole != null) {
            return MCollection.contains(withRole.value(), role);
        }
        return false;
    }

    public static boolean authenticate(String username, String password) {
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        if (request == null) {
            // This is in a background thread and we can't access the request to
            // log in the user
            return false;
        }
        try {
            request.login(username, password);
            // change session ID to protect against session fixation
            request.getHttpServletRequest().changeSessionId();
            return true;
        } catch (ServletException e) {
            // login exception handle code omitted
            return false;
        }
    }

    public static void logout() {
        UI.getCurrent().getPage().setLocation(LOGOUT_SUCCESS_URL);
        VaadinSession.getCurrent().getSession().invalidate();
    }

}
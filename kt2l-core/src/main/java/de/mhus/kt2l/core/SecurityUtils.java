package de.mhus.kt2l.core;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import de.mhus.commons.tools.MCollection;
import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.realm.GenericPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class SecurityUtils {

    private static final String LOGOUT_SUCCESS_URL = "/";

    public static boolean hasPrincipalRoles(Object resource, Set<String> roles) {
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
    public static boolean hasPrincipalRoles(Set<String> roles) {
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        if (request == null) {
            LOGGER.warn("Request not found");
            return false;
        }
        final var principal = request.getUserPrincipal();
        if (principal == null) {
            LOGGER.warn("Principal not found in request");
            return false;
        }

        if (principal instanceof GenericPrincipal user) {
            for (String resourceRole : roles) {
                if (!user.hasRole(resourceRole)) return false;
            }
            return true;
        }

        if (principal instanceof Authentication user) {
            final var authorities = user.getAuthorities();
            if (authorities == null) {
                LOGGER.warn("Authorities of principal {} not set", principal);
                return false;
            }
            for (String resourceRole : roles) {
                boolean found = false;
                for (GrantedAuthority authoritity : authorities)
                    if (toAuthorityRoleName(resourceRole).equals(authoritity.getAuthority())) {
                        found = true;
                        break;
                    }
                if (!found) return false;
            }
            return true;
        }

        LOGGER.warn("Unknown principal type {}", principal.getClass());
        return false;

    }

        // do not use
    static boolean hasPrincipalResourceRoles(Object resource) {
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        if (request == null) {
            LOGGER.warn("Request not found");
            return false;
        }
        final var principal = request.getUserPrincipal();
        if (principal == null) {
            LOGGER.warn("Principal not found in request");
            return false;
        }

        final var withRole = resource.getClass().getAnnotation(WithRole.class);
        if (withRole == null) return false;

        if (principal instanceof GenericPrincipal user) {
            for (ROLE resourceRole : withRole.value()) {
               if (!user.hasRole(resourceRole.name())) return false;
            }
            return true;
        }

        if (principal instanceof Authentication user) {
            final var authorities = user.getAuthorities();
            if (authorities == null) {
                LOGGER.warn("Authorities of principal {} not set", principal);
                return false;
            }
            for (ROLE resourceRole : withRole.value()) {
                boolean found = false;
                for (GrantedAuthority authoritity : authorities)
                    if (toAuthorityRoleName(resourceRole.name()).equals(authoritity.getAuthority())) {
                        found = true;
                        break;
                    }
                if (!found) return false;
            }
            return true;
        }

        LOGGER.warn("Unknown principal type {}", principal.getClass());
        return false;

    }

    private static String toAuthorityRoleName(String name) {
        return "ROLE_" + name.toUpperCase().trim();
    }

    // do not use
    static boolean hasPrincipalResourceRoles(Object resource, ROLE role) {
        if (resource == null) return false;
        final var withRole = resource.getClass().getAnnotation(WithRole.class);
        if (withRole != null) {
            return MCollection.contains(withRole.value(), role);
        }
        return false;
    }

    public static boolean isAuthenticated() {
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        return request != null && request.getUserPrincipal() != null;
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
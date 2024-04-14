package de.mhus.kt2l.core;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import de.mhus.commons.tools.MSystem;
import de.mhus.kt2l.config.AaaConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

@Component
public class SecurityService {

    @Autowired
    private AaaConfiguration configuration;

    private static final String LOGOUT_SUCCESS_URL = "/";

    public Principal getPrincipal() {
        return SecurityUtils.getPrincipal();
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
        final var roles = configuration.getRoles(resourceScope, MSystem.getClassName(resource));
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
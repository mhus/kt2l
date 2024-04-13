package de.mhus.kt2l.resources;

import de.mhus.kt2l.core.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

@Component
public class ActionService {

    @Autowired(required = false)
    private Collection<ResourceAction> actions;

    @Autowired
    private SecurityService securityService;


    public Collection<ResourceAction> findActionsForResource(String resourceType) {
        if (actions == null) return Collections.emptyList();
        return actions.stream().filter(a -> hasAccess(a) && canHandle(resourceType, a)).toList();
    }

    private boolean canHandle(String resourceType, ResourceAction a) {
        return a.canHandleResourceType(resourceType);
    }

    private boolean hasAccess(ResourceAction a) {
        return securityService.hasRole(a);
    }

}

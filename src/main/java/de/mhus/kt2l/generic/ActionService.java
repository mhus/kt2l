package de.mhus.kt2l.generic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

@Component
public class ActionService {

    @Autowired(required = false)
    private Collection<ResourceAction> actions;

    public Collection<ResourceAction> findActionsForResource(String resourceType) {
        if (actions == null) return Collections.emptyList();
        return actions.stream().filter(a -> a.canHandleResourceType(resourceType)).toList();
    }

}

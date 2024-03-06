package de.mhus.kt2l;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
public class ActionService {

    @Autowired(required = false)
    private Collection<XUiAction> actions;

    public Collection<XUiAction> findActionsForResource(String resourceType) {
        if (actions == null) return Collections.emptyList();
        return actions.stream().filter(a -> a.canHandleResourceType(resourceType)).toList();
    }

}

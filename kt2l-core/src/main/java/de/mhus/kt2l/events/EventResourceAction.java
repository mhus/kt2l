package de.mhus.kt2l.events;

import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EventResourceAction implements ResourceAction {
    @Override
    public boolean canHandleResourceType(String resourceType) {
        return false;
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return false;
    }

    @Override
    public void execute(ExecutionContext context) {

    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public int getMenuOrder() {
        return 0;
    }

    @Override
    public String getShortcutKey() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }
}

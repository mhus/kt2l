package de.mhus.kt2l.resources;

import io.kubernetes.client.common.KubernetesObject;

import java.util.Set;

public interface ResourceAction {
    boolean canHandleResourceType(String resourceType);
    boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected);
    void execute(ExecutionContext context);
    String getTitle();
    String getMenuBarPath();
    String getShortcutKey();
    String getPopupPath();

    String getDescription();
}

package de.mhus.kt2l.resources;

import com.vaadin.flow.component.icon.VaadinIcon;
import io.kubernetes.client.common.KubernetesObject;

import java.util.Set;

public interface ResourceAction {
    int ACTIONS_ORDER = 1000;
    String ACTIONS_PATH = "Actions;icon=" + VaadinIcon.START_COG;
    int VIEW_ORDER = 2000;
    String VIEW_PATH = "View;icon=" + VaadinIcon.EYE;
    int TOOLS_ORDER = 5000;
    String TOOLS_PATH = "Tools;icon=" + VaadinIcon.TOOLS;

    boolean canHandleResourceType(String resourceType);
    boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected);
    void execute(ExecutionContext context);
    String getTitle();
    String getMenuPath();
    int getMenuOrder();
    String getShortcutKey();

    String getDescription();
}

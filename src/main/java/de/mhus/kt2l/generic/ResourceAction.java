package de.mhus.kt2l.generic;

import java.util.Set;

public interface ResourceAction {
    boolean canHandleResourceType(String resourceType);
    boolean canHandleResource(String resourceType, Set<? extends Object> selected);
    void execute(ExecutionContext context);
    String getTitle();
    String getMenuBarPath();
    String getShortcutKey();
    String getPopupPath();

    String getDescription();
}

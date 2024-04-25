---
sidebar_position: 12
title: Resource Actions
---

# Resource Actions

Resource actions are actions that can be performed on resources in the UI. To provide a new resource action, you need to
provide a new Service implementing the `ResourceAction` interface. The service itself has no IU representation, but it
can be used to provide new panels or dialogs.

```java
public interface ResourceAction {
    boolean canHandleResourceType(String resourceType);
    boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected);
    void execute(ExecutionContext context);
    String getTitle();
    String getMenuPath();
    int getMenuOrder();
    String getShortcutKey();
    String getDescription();
}
```

If a new ResourceGrid is created all available ResourceActions are collected that can handle the resource type.
The actions will be displayed as buttons in the toolbar and as context menu items. ResourceActions can be used 
on other places in the UI as well.

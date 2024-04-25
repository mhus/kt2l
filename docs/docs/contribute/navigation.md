---
sidebar_position: 7
title: Navigation
---

# Navigation

The mainView provides a navigation bar at the left side of the screen. The navigation bar is a Tab component that
displays the available views. To add a new view to the navigation bar, you need to use the `PanelService` to register
new views.

```java
panelService.addPanel(
        context.getSelectedTab(),
        context.getClusterConfiguration().name() + ":" + selected.getMetadata().getNamespace() + "." + selected.getMetadata().getName() + ":logs",
        selected.getMetadata().getName(),
        true,
        VaadinIcon.FORWARD.create(),
        () ->
                new PodExecPanel(
                        context.getClusterConfiguration(),
                        context.getApi(),
                        context.getMainView(),
                        containers
                )).setHelpContext("exec").select();
```


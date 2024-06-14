---
sidebar_position: 7
title: Navigation
---

# Navigation

The Core provides a navigation bar at the left side of the screen. The navigation bar is a Tab component that
displays the available views. To add a new view to the navigation bar, you need to use the `PanelService` to register
new views.

## Panels

If you create a new panel add the code to create the panel to the `PanelService`. Use method signatures like `show...Panel`
for unique panels and otherwise `add...Panel`.

Panels will be added to the navigation bar and the UI panel will be added to the content layout. If the tab
is not selected the panel will be hidden. To save memory you can define the panel `reproducable`. In this
case the panel will be removed from content while it is not selected.

Add a help context and a help section in `help.yaml` to provide help for the new panel.

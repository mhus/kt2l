---
sidebar_position: 22
title: Resources Panel
---
# Resources

In the first row you have filter options. You can select the resource types and the namespace and a name filter to see the resources in the cluster. If a special filter is active
the left Filter Icon is highlighted. Click it to destroy the special filter. All filters are active

If the name filter starts with `/` the expression will be interpreted as regular expression elswise it will be interpreted as a simple string match.

In the next row you can see the available actions for the resource type. Use the shortkeys if you
have focus in the grid. Moreover there are the following shortkeys available:

- `Meta + R` to refresh the resources grid
- `Ctrl + /` to focus the name filter
- `Ctrl + :` to focus the resource type filter
- `Ctrl + '` to focus the namespace filter

***NOTE:*** The grey resource types have no specialized grid implemented yet.

## Pod Score

The pod score is a simple heuristic to give you a hint how healthy a pod is. A higher score is more problematic.
You can toggle scorer alert highlighting in the `View' menu `Highlight Alerts`. You can see the flag `(HA)` in the
status bar if highlighting is enabled.

For more details see the [Pod Score Comnfiguration](../configuration/config-pod_-scorer.md) section.

## Duplicate Resources Grid

The duplicate resources grid shows you resources with the same name and namespace. 

## Status Line

The status line shows you the number of resources in the grid and the number of selected resources.
If resources are filtered the number of filtered resources is shown as well.
The flag `(HA)` indicates that alerts will be highlighed.

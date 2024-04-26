---
sidebar_position: 10
title: Cluster Actions
---

# Cluster Actions

To provide actions for a cluster in the `ClusterOverviewPanel` you can provide Beans implementing 
the `ClusterAction` interface. The `ClusterAction` interface provides a method to get the name of
the action and a method to execute the action. 

## How to implement a Cluster Action

To implement a new Cluster Action you need to create a new class that implements the `ClusterAction` interface.

```java
public interface ClusterAction {

    String getTitle();

    void execute(Core core, ClusterOverviewPanel.Cluster cluster);

    Icon getIcon();

    int getPriority();
}
```

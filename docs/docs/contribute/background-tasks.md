---
sidebar_position: 11
title: Background Tasks
---

# Background Tasks

To provide cluster related tasks in the background we use a system to run tasks in the background. If all tabs for
a cluster are closed this tasks will be removed from the background task system. The most common use case is to
watch the cluster for changes of resources.

Only one instance of a background task is running for a cluster. Use Background Tasks only if it makes sense to have
a singleton instance for a cluster. The job will live as long as the cluster is open in the UI.

## How to implement a background task

To register a new Background Task you need to call the `getBackgroundJob` method of the `MainView` class and
provide a creator lambda. The creator lambda will be called if the background task is not running for the cluster.

```java
mainView.getBackgroundJob(clusterConfig.name(), ClusterNodeWatch.class, () -> new ClusterNodeWatch())
```
The Background Task class must implement the `ClusterBackgroundJob` interface. 






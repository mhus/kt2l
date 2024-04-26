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

To register a new Background Task you need to call the `getBackgroundJob` method of the `Core` class and
provide a creator lambda. The creator lambda will be called if the background task is not running for the cluster.

```java
core.getBackgroundJob(clusterConfig.name(), ClusterNodeWatch.class, () -> new ClusterNodeWatch())
```

Best practice is to provide a static instance() method to get the instance of the background task.

```java
public static ClusterNodeWatch instance(Core core, Cluster clusterConfig) {
    return core.getBackgroundJob(clusterConfig.name(), ClusterNodeWatch.class, () -> new ClusterNodeWatch());
}
```

and deny the creation of the class with a private constructor.

```java
private ClusterNodeWatch() {
}
```

The Background Task class must extend from abstract `ClusterBackgroundJob` class. 

```java
public abstract class ClusterBackgroundJob {
    public abstract void close();
    public abstract void init(Core core, String clusterId, String jobId) throws IOException;
}
```





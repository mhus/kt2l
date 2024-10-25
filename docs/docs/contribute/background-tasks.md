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

Best practice is to use `backgroundJobInstance()` method to get the instance of the background task.

```java
var job = core.backgroundJobInstance(cluster, ClusterNodeWatch.class);
```

The Background Task class must extend from abstract `ClusterBackgroundJob` but in the most cases from 
`AbstractClusterWatch` is a better choice. The `AbstractClusterWatch` class simplifies the implementation 
of a background task.

```java
public abstract class AbstractClusterWatch<V1Namespace> {

}
```

## How to use background jobs

Background job is a special kind of background task and is managed by the `BackgroundJobDialogRegistry` class.
A background job is always a graphical progress dialog. You can create it by using the `BackgroundJobDialog`.

If you allow canceling you should check the `isCanceled()` method to stop the job properly.

```java
var dialog = new BackgroundJobDialog(core, cluster, true);
dialog.setHeaderTitle("My Background Job");
dialog.open();

Thread.startVirtualThread(() ->{
        while(!dialog.isCanceled()){
        // do something
        }
        context.getUi().access(()->dialog.close());
});
```
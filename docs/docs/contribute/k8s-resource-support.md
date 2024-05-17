---
sidebar_position: 6
title: Kubernetes Resource Support
---

# Kubernetes Resource Support

To support handling Kubernetes resources in the background, you can implement the `HandlerK8s` interface and provide
a new Service for the resource type.

```java
public interface HandlerK8s {

    K8s getManagedResource();

    String getPreview(ApiProvider apiProvider, KubernetesObject res);

    void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException;

    Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException;

    Object create(ApiProvider apiProvider, String yaml) throws ApiException;

    <L extends KubernetesListObject> L createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException;

    <L extends KubernetesListObject> L createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException;

    Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException;

    [...]
}
```


---
sidebar_position: 6
title: Kubernetes Resource Support
---

# Kubernetes Resource Support

To support handling Kubernetes resources in the background, you can implement the `KHandler` interface and provide
a new Service for the resource type.

```java
public interface KHandler {

    String getManagedKind();

    void replace(CoreV1Api api, String name, String namespace, String yaml) throws ApiException;

    void delete(CoreV1Api api, String name, String namespace) throws ApiException;

}
```


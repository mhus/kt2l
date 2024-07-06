---
sidebar_position: 2
title: Clusters
---

# Clusters Configuration

In the file `clusters.yaml` you can define the clusters you want to use in the application. By default, the
configuration can be overwritten in the user config directory. Set the environment variable 
`KT2L_PROTECTED_CLUSTERS_CONFIG` to `true` to protect the configuration from being overwritten.

```yaml
defaultCluster: colima
defaultResourceType: "pods"
defaultNamespace: default
clusterSelector: true
clusters:
  - name: colima
    title: "Colima"
    enabled: true
    color: green
```

The example shows the configuration for the cluster `colima`.

The following colors are available: `red`, `green`, `blue`, `yellow`, `purple`, `orange`, `pink`, `cyan`, `gray`.

The common confiuration properties are:
- `defaultCluster`: The default cluster
- `defaultResourceType`: The default resource type
- `defaultNamespace`: The default namespace
- `clusterSelector`: If the cluster selector is enabled or the default cluster forced to be used (defaultCluster must be set)

In the cluster section you can define the following properties:

- `name`: The name of the cluster
- `title`: The title of the cluster
- `enabled`: If the cluster is enabled
- `color`: The color of the cluster title
- `defaultNamespace`: The default namespace for the cluster
- `defaultResourceType`: The default resource type for the cluster
- `shell`: A list of pods and the shell command to connect to the pod

If you are inside a kubernetes cluster, the cluster name is `.local-cluster`.

If you want to reduce the number of clusters in the cluster list, you can configure the access rights in `aaa.yaml`
for cluster resources.

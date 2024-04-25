---
sidebar_position: 1
title: Authorization Configuration
---

# Authorization Configuration

The authorization configuration is used to configure the role permissions on resources. The configuration is stored in the `config/aaa.yaml` file.

```yaml
default:
  resource: READ
  namespace: READ
  cluster: READ
  cluster_action: READ
resource_action:
resource:
resource_grid:
namespace:
cluster:
```

In the sections are resource types. Under each are the permissions for the resource 
ids. The `default` section is used for the default permissions for types. 
Define a resource id and a comma separated list of roles. The roles are the permissions
that are needed to access the resource with the given id.

Some resource types have default actions define for each resource itself other onec
need a default configured in the `default` section.

The resource id can be a class name or a specific id. In case of `resource` is the
kubernetes resource plural. In case of `namespace` is the namespace name.

Special resource ids are:

* resource_action: "de.mhus.kt2l.resources.ResourceDetailsPanel_write": WRITE


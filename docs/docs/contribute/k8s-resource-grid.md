---
sidebar_position: 4
title: Kubernetes Resource Grid
---

# Kubernetes Resource Grid

You can extend the ResourceGridPanel with new View implementations for resource kinds. To provide a new
View implementation you need to create a new Service that extends the `ResourceGrid` class. All grids
should have the same behavior and should be able to filter and sort the resources. It is
recommended to extend the `AbstractWithoutNamespacGrid` or `AbstractWithNamespacGrid` class.

For a simple example see the `NodesGrid` class. `PodGrid` is a very complex example that uses the
`AbstractWithNamespacGrid` and provide a preview panel.

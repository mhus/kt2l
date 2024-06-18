---
sidebar_position: 13
title: Generators
---

# Source Code Generators

## K8s Resource Types

In some cases the project is driven by source code generators. This is the case for Kubernetes Resource Types
which are supported by default. The generator is (currently) a separate Main_Class in kt2l-core test sources.

By default, the generator is not executed. It can be executed manually but the generated classes are part
of the project and are not generated during the build process.

## Version Information

The file DeployInfo will be created during the build process. It contains the version information of the project.


---
sidebar_position: 13
title: Generators
---

# Source Code Generators

## K8s Resource Types

Parts of the project are driven by source code generators. This is the case for default Kubernetes Resource 
Types. The generator is a separate module project `kt2l-generator` and contains a main class to generate
the sources. If needed the K8s java file must be duplicated into the `kt2l-generator` project before the
generator is executed.

By default, the generator is not executed in the build process. Generated java files are part of the 
source repository.

## Version Information

The file DeployInfo will be created during the build process. It contains the version information of the project.


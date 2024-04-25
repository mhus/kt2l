---
sidebar_position: 3
title: Shell Configuration
---

# Shell Configuration

The shell configuration is used to find the shell that will be used to run the commands in the local terminal or
in pod containers.

```yaml
shell: /bin/bash
default: /bin/sh
images:
contains:
    ubuntu: /bin/bash
    centos: /bin/bash
```

The `shell` key is used to specify the shell that will be used to run the commands in the local terminal or in pod 
containers. All other keys are used to specify the shell that will be used to run the commands in the pod containers.

Shells can also be defined in the `cluster` configuration in section `shell`. If a entry in the `images` section matches
the image of the pod container, the shell defined in the `images` section will be used. If no entry in the `images` section
matches the image of the pod container, the shell defined in the `contains` can match the image of the pod container.
If no entry in the `contains` section matches the image of the pod container, the shell defined in the `default` key will 
be used. Finally, if no shell is defined in the `default` key, the shell `/bin/sh` will be used.

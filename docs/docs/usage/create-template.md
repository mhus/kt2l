---
sidebar_position: 42
title: Resource Creation Templates
---

# Resource Creation Templates

In the resource creation editor it is possible to define resources using yaml definitions. If you use the snippet view
from the help menu you can insert predefined snippets for resources. To provide flexle snippets it is possible to
define placeholders and template instructions in the snipped. Each resource (separator is `---`) can have its own
placeholders.

Templates are comments in the yaml file. The template is defined by a comment starting with `# TEMPLATE BEGIN`. 
The template ends with a comment `# TEMPLATE END`. The template contains in each line a definition for a placeholder.

Placeholders are defined by a comment starting with `# name : type (options) <description> : value`. The name is the
name which can be replaced with `${name}` in the yaml definition.

Types are
- `string` for a string value
- `integer` for a number value
- `boolean` for a boolean value, avid are `true` and `false`
- `options` for a value from a list of comma separated options

Options and descriptions are optional. The value is the default value for the placeholder and is also optional.

The placeholder `namespace` is always available and is the namespace of the resources creation panel.

Example:

```yaml
# TEMPLATE BEGIN
# name: string <Name of the pod> : mypod
# TEMPLATE END
apiVersion: v1
kind: Pod
metadata:
  name: ${name}
  namespace: ${namespace}
spec:
    containers:
    - name: ${name}
      image: busybox
```



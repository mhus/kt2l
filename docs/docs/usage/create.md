---
sidebar_position: 13
title: Create Panel
---
# Create

Create resources using yaml definitions.

For details see the Kubernetes documentation or use the Snippets feature in the help menu.

Separate different resources with `---`. Each resource can define a template definition to substitute values
in the yaml definition. The template definition is a yaml comment.

```yaml
# TEMPLATE BEGIN
# name: string <Name of the pod>:myname
# TEMPLATE END
```

For the syntax of the template definition see [Create Template](create-template.md).

Use the `Template` menu to manage template values. If you have enabled templates then
the yaml will be substituted with the template values. If you use the Dollar sign `$` in the 
yaml definition you have to replace it with `$$` to avoid substitution.

The `Delete` button will try to delete the selected resource in reverse order.

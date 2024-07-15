---
sidebar_position: 18
title: Create Panel
---
# Patch

Patch resources using yaml definitions.

For details see the Kubernetes documentation or use the Snippets feature in the help menu.

Examples:

```json
[
  {"op":"replace","path":"/1/test","value":"test32132"},
  {"op":"remove","path":"/2/test"},
  {"op":"add","path":"/2/test1","value":"test321"},
  {"op":"replace","path":"/3/test/2","value":3},
  {"op":"add","path":"/4","value":{"test":[1,2,3]}}
]
```

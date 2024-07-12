---
sidebar_position: 15
title: Edit ConfigMap
---
# Edit ConfigMap

This editor allows you to edit entries in a ConfigMap. You can add, edit, or delete entries in the ConfigMap.

After saving the changes, the ConfigMap will be updated in the cluster. But you have to restart pods that use the 
ConfigMap to apply the changes. This will not be done automatically by kubernetes.

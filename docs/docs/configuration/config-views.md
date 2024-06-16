---
sidebar_position: 4
title: Views
---

# Configuring Views

All views that not have a separate configuration file are configured in the `config/views.php` file. This file contains 
an array of view configurations. Each view configuration is an array with keys.

Example:
```yaml
log:
  maxCachedEntries: 1000
  maxCachedCharacters: 30000
```

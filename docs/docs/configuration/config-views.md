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

## Core idle auto extend

A session is considered idle if no user interaction has been detected for a certain amount of time. It will
also be lost if the browser is closed or navigated to another page.

In this case it's important to have a short session idle timeout to free up resources. Otherwise, 
the a short session idle timeout will force the user to intercat with the application more often.
If the user is working on a long task, the session will be lost and the user will have to log in again and
loose the current work.

to solfe this problem, the session idle timeout can be extended automatically by the idle notification dialog.
The dialog will appear a few seconds before the session is lost and will extend the session if the user interacts 
with it or if auto extend is enabled. The auto extend feature is enabled by default.

The default session timeout is set to 5 minutes but Idle Notification timeout to 120 seconds. The idle notification dialog 
will appear 90 seconds before the session is lost and auto extend after 1 second.

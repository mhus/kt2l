---
sidebar_position: 2
title: General
---

# General

## Work with timing out resources

"Timing out resources" typically means that certain resources, like connections or processes, are being automatically 
closed or terminated after a set period of inactivity or upon reaching a predetermined time limit. You could rephrase
it as "automatically closing resources after a period of inactivity" or "terminating resources if they're idle for too long."

To handle the scenario where the user refreshes the browser, causing the page to reload and the Vaadin UI to be recreated,
you need to ensure that you always fetch the current UI instance from the core whenever it's needed anew.

The kubernetes client is only valid for a short time. Do not store the api client or api objects using the client for
a long time. Always get the client new from a `ClientApiProvider`.

## Close resources

It's important to close resources when they're no longer needed to free up memory and prevent resource leaks. Resources
must be closed if a tab or a cluster is closed by the user or if the Vaadin UI session is closed.

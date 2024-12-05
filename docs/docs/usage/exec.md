---
sidebar_position: 17
title: Exec Panel
---
# Exec

This panel allows you to execute a command script on one or more pods in the same time. Insert the script
in the text area and click on the `Run` button. You can select the affected pods by clicking on the 
`Resource Selector` before you run the script.

A script contains of remote executions, commands and comments. A remote execution is a 
command that is executed with the exec cmd="..." command and will be executed
remotely.

A command is a command that is interpreted by kt2l. A command must start with a `!` character.

Commends are comments that are ignored by the interpreter. A comment must start with a `#` character.

[Commands](commands.md)

In the `Store` menu you are able to access and download stored files in the `Storage` panel.

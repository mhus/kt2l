---
sidebar_position: 2
title: Local command execution
---

# Local command execution

The local command execution is used to describe how to execute commands on the local system. 
The configuration is stored in the `config/cmd-[os system].yaml` file.

OS systems are `linux`, `windows` and `macos`.

For each executed command you can define a list of arguments that is used to execute the command.

cmd-macos.yaml:
```yaml
terminal:
  - /usr/bin/osascript
  - '-e'
  - tell application "Terminal" to do script "kubectl exec -it --context '${context}' -c '${container}' '${pod}' -- '${cmd}'; exit"
  - '-e'
  - tell application "Terminal" to activate
webbrowser:
  - /usr/bin/open
  - '${url}'
filebrowser:
  - /usr/bin/open
  - '-R'
  - '${path}'
```

Depending on the command you can use the following variables:
- `${context}` - the current kubernetes context
- `${container}` - the container name
- `${pod}` - the pod name
- `${cmd}` - the command to execute
- `${namespace}` - the namespace name
- `${url}` - the url to open
- `${path}` - the path to open
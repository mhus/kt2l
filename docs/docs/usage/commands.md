---
sidebar_position: 40
---

# Commands

## EXEC

Run a command like `kubectl exec` direct or with shell and opens a new terminal for it.
You can either use the argument `cmd` or `exec` to define the command. If you do not
define both a shell with prompt will be opened.

Arguments:
- cmd: string - the command to run as a shell script.
- exec: string - the execution command directly, parts are comma separated.
- tty: boolean - open a tty for the command, default is `true`.
- stdin: boolean - opens a stdin pipeline for the command, default is `true`.
- shell: string - command for the shell, default is `/bin/bash`.

The command will create a `scope` to handle the open terminal.

## ATTACH

Attach to a pod like `kubectl attach`.

Arguments:
- tty: boolean - open a tty for the command, default is `true`.
- stdin: boolean - opens a stdin pipeline for the command, default is `true`.

The command will create a `scope` to handle the open terminal.

## CLEAR

Clears the terminal content buffers of the `scope`.

## SEND

Send characters to an open terminal.

Arguments:
- line: string - send a line inclusive a newline character
- msg: string - send a message without the newline character

## WAIT

Wait for a specific content to appear in the terminal or the end of the terminal connection.
You can wait for `content` (combined standard out and error out) or 
one of `stdout` or `errout`. Or all of them.

Arguments:
- content: string - the content to wait for in the terminal content buffer.
- stdout: string - the content to wait for in the terminal stdout buffer.
- stderr: string - the content to wait for in the terminal stderr buffer.
- timeout: number/period - the timeout period to wait for the content to appear, e.g. 5s, 10m, 1h

Return parameter (&lt;scope&gt;.return):
- `closed` - if the terminal was closed
- `timeout` - on timeout
- `content` - Match appears in content
- `stdin` - Match appears in stdin
- `stderr` - Match appears in stderr

## CLOSE

Closes the current terminal in the `scope`.

## DEBUG

Starts a new Ephemeral container for debugging and creates a new scope with attach or exec.

Arguments:
- cmd: string - the command to run in the new container, comma separated.
- tty: boolean - open a tty for the command, default is `true`.
- stdin: boolean - opens a stdin pipeline for the command, default is `true`.
- exec: string - the execution command comma separated. If this is set an exec will be created otherwise an attach scope.

Return parameters (&lt;scope&gt;.*):
- `container` - Name of the created container in the pod.

Note: The container will not be removed after the command is finished.

## ECHO

Echo a message to the local output.

Arguments:
- msg: string - The content to print

## SET

Set context values / environment variables

Arguments:
- key: string
- value: string

## IF/ELSEIF/ELSE/ENDIF

Conditional statements.

Arguments for IF/ELSEIF:
- is: string - the condition to evaluate

Conditions:
- Use `${name}` for substitution
- Use `< <= > >= == != =~` for comparison
- Use `|| &&` to connect multiple comparisons

## SLEEP

Sleep a amount of time.

Arguments:
- time : string - time to sleep period.

## ENV

Set an environment variable or if not set print all variables.

Arguments:
- key: string
- value: string

## DOWNLOAD

Download a file from the pod to the local storage.

Arguments:
- from: string - the file to download
- to: string - the target file (ths system will add a timestamp to the file name)

# Scope

Using communication commands to interact with the terminal you can use scopes to separate different sessions.
Add the scope to the command name with a dot before the command, e.g. `scope.exec` and `scope.send`, `scope.close`.
If no scope is provided the default scope `default` is used.

# Environment variables

The variables are global for all sessions.

Variables:
- `container` - Name of the container in the pod to connect to.
- '&lt;scope&gt;.return' - Some commands will return a result in this variable.

# Examples

1. Open a terminal with shell, send `ls -l`, wait for the next prompt and close the terminal.

```
!EXEC
!CLEAR
!SEND line="ls -l"
!WAIT content="root@nginx"
!CLOSE
```

2. Open a terminal with the shell command `ls -la` and wait for the terminal to close. Also print
   the process list and wait.

```
ls -la
!wait
ps
!wait
```

3. Attaches to a terminal and send in the background a SIGHUP signal to the first process. Wait
   for the output `startup` in the terminal.

```
session1.ATTACH stdin=false tty=false
session2.EXEC cmd="kill -1 1"
session1.WAIT content="startup"
session1.CLOSE
session2.CLOSE
```


## Commands

* EXEC - run a command as a shell script and opens a new terminal
  cmd: string - the command to run as a shell script
  exec: string - the execution command directly, comma separated
* CLEAR - clear the terminal contnt buffer
* SEND - send a command to an open terminal
  line: string - send a line inclusive of the newline character
  msg: string - send a message without the newline character
* WAIT - wait for a specific content to appear in the terminal or the end of the terminal connection
  content: string - the content to wait for in the terminal content buffer
  timeout: number/period - the timeout period to wait for the content to appear, e.g. 5s, 10m, 1h
* CLOSE - close the terminal connection
* ECHO - echo a message to the local output
  msg: string
* SET - set context values / environment variables
  key: string
  value: string
* ATTACH - attach to a terminal session
* IF/ELSEIF/ELSE/ENDIF - conditional statements
  is: string - the condition to evaluate

## Scope

Using communication commands to interact with the terminal you can use scopes to separate different sessions.
Add the scope to the command name with a dot before the command, e.g. `scope.exec` and `scope.send`, `scope.close`.
If no scope is provided the default scope `default` is used.

## Examples

```
EXEC
CLEAR
SEND line="ls -l"
WAIT content="root@nginx"
CLOSE
```
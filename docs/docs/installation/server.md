---
sidebar_position: 7
---

# Server

## Install Java

The server should run on every target platform where Java 21 is available. First install
Java JRE 21 from oracle or Eclipse [Temurin Latest Release](https://adoptium.net/de/temurin/releases/).

if the command `java --version` returns something like ***java 21.0.2 2024-01-16 LTS*** it
looks good for now. Be sure the java home variable JAVA_HOME is set to the correct path.
```$JAVA_HOME/bin/java --version``` should result the same output then before.

## Download the server

Download the latest ***Server Bundled*** ZIP-File from the website [TK2L Website](https://tk2l.org#downloads).

## Unzip the file

Use the unzip command to unzip `unzip kt2l-server-...zip`.

## Control the server

Navigate to the unzipped folder and start the server with the command `./bin/service.sh start`.

The script `service.sh` is a wrapper script for the server. It is possible to:

- start the server with `./bin/service.sh start`
- stop the server with `./bin/service.sh stop`
- restart the server with `./bin/service.sh restart`
- show the server status with `./bin/service.sh status`
- kill the server with `./bin/service.sh kill`
- cleanup the server pid with `./bin/service.sh zap`
- show the server log with `./bin/service.sh logs`

## Configure the server startup

The server can be configured with the file `env.sh`. The file is located in the root folder of the server.
You can set environment variables for the server in this file.

- JAVA_HOME - the path to the java home folder (not the binary folder)
- PID_FILE - the path to the pid file
- LOG_FILE - the path to the log file
- SERVER_PORT - the port the server is listening on (default 8080)
- KT2L_ROTATE_LOGS - set to `true` to rotate the logs, otherwise `false` or leave empty
- CONFIGURATION_DIRECTORY - the path to the configuration directory (can't be overwritten)
- CONFIGURATION_LOCAL_DIRECTORY - the path to the local configuration directory (to overwrite the default configuration)
- CONFIGURATION_USER_DIRECTORY - the path to the user configuration directory (to overwrite the default configuration for each user)
- KT2L_TMP_DIRECTORY - the path to the temporary directory (default is the java system temporary directory)

## Update the server

To update the server, download the latest ***Server Bundled*** ZIP-File from the website [TK2L Website](https://tk2l.org#downloads) and
unzip the file into a temporary folder. Copy the content of the folder ./bin/ to the server folder and overwrite the existing binaries.

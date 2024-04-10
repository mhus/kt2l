---
sidebar_position: 8
---

# Run Container

The simplest way to run the server is to use the container bundle.

Prepare a local directory for config files:

```bash
mkdir -p ~/.kt2l/config
```
On mac it is a problem that the userid inside the container is 1001 and not 
501 (default on mac). This means the process in the container can't access
files in the new config directory. use `chmod -R 777 ~/.kt2l/config` to give
access. But keep in mind that the files have different user rights.

The following command will run the server in the background using you kube config:

```bash

docker run -d --rm --name kt2l-server \
    -p 8080:8080 \
    -v "$HOME/.kube:/home/user/.kube" \
    -v "$HOME/.kt2l/config:/home/user/config" \
    -e CONFIGURATION_DIRECTORY=/home/user/config \
    --platform linux/amd64 \
    mhus/kt2l-server:snapshot
```

You can access the server with the browser `http://localhost:8080`. You have to login if you
use the container version. Use ***admin*** and the password from the container log. After a
few seconds you can get the password with the following command: 
`docker logs kt2l-server|grep "Set login password for user admin"|cut -d \} -f 2|cut -d ' ' -f 1`.

To stop and remove the docker container simply run `docker stop kt2l-server`.

---
sidebar_position: 8
---

# Run Container

The simplest way to run the server is to use the container bundle.

The following command will run the server in the background using you kube config:

```bash
mkdir -p ~/.kt2l/config

docker run -d --name kt2l-server \
    -p 8080:8080 \
    -v "$HOME/.kube:/home/user/.kube" \
    --platform linux/amd64 \
    kt2l-server:snapshot
```

You can access the server with the browser `http://localhost:8080`. You have to login if you
use the continer version. Use ***admin*** and the password from the container log. You can get
the password with the following command: `docker logs kt2l-server|grep admin`
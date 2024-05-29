# Port Forwarding

Port forwarding is a way to make a pod or service available to the local machine. This is useful for debugging or 
accessing applications running in a cluster.

## Forwarding Syntax

Add one or more lines in the command text area. Each line should be in the following format:

```
<type> <namespace> <name> <service poryt> <local-port> [action]
```

- `<type>`: `pod` or `svc` for pod or service.
- `<namespace>`: The namespace of the pod or service.
- `<name>`: The name of the pod or service.
- `<service port>`: The port of the service.
- `<local-port>`: The local port to listen to.
- `[action]`: Optional. `on` or `off` to control the forwarding.

If the forwarding already exists, the command will only execute the action if set.

***Important:*** The local port must be unique for each forwarding and not be used by another local service. It's 
possible to define more than one service on the same port but only one can be started at a time.

***Important:*** If the remote pod or service exists will be checked if the first client connection to the local port 
is successful.

If a new forwarding is created, it will be disabled by default. Use the `on` action to enable it instantly.

## Examples

```
pod default my-pod 8080 9000 on
svc default my-svc 8080 9001 on
```

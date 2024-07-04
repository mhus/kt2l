# Local Kubernetes Environment

## Deploy to Kubernetes

Modify image tag in deployment.yaml

```bash
kubectl apply -f namespace.yaml
kubectl apply -f clusterrole.yaml
kubectl apply -f serviceaccount.yaml
kubectl apply -f clusterrolebinding.yaml
kubectl apply -f environment.yaml
kubectl apply -f config.yaml
kubectl apply -f passwords.yaml
kubectl apply -f deployment.yaml
```

Optional - Expose service

```bash
kubectl apply -f service.yaml
```

## Accessing the service

Change the pod name in the following command

```bash
kubectl port-forward -n kt2l pods/kt2l-7695fb4478-qsvng 9000:9080
```

## Delete deployment only

```bash
kubectl delete -n kt2l deployments.apps kt2l
```

## Delete all

```bash
kubectl delete ns kt2l
```
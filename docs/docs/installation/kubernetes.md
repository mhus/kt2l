---
sidebar_position: 3
title: Kubernetes Installation
---

# Kubernetes Installation

Quick: Copy and modify the file kt2l-server/launcher/k8s/deployment.yaml to your own k8s deployment file. Then run the following command:

```bash
kubectl apply -f deployment.yaml
```

# Use helm to install kt2l

To install kt2l using helm, run the following commands:

```bash
helm repo add kt2l http://kt2l.org

helm install kt2l kt2l/kt2l-chart
```
The basic installation will not work out of the box. You should use a custom values file to customize the installation. 
You can find an example value files in the kt2l-server/launcher/k8s/

```yaml
helm install kt2l kt2l/kt2l-chart --values https://raw.githubusercontent.com/mhus/kt2l/main/kt2l-server/launcher/k8s/values-simple.yaml
```

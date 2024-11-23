#!/bin/bash

if [ ! -f /root/.kube/config-minikube ]; then
  echo "Info: No kubeconfig found in /root/.kube/config-minikube"
  exit 0
fi
cp /root/.kube/config-minikube /home/user/.kube/config-minikube
cp -r /root/.minikube /home/user/.minikube

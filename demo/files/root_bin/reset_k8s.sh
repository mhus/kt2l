#!/bin/bash

kubectl delete deployment -n default --all  --kubeconfig /root/.kube/config
kubectl delete cronjob -n default --all  --kubeconfig /root/.kube/config
kubectl delete statefulset -n default --all  --kubeconfig /root/.kube/config
kubectl delete service -n default --all  --kubeconfig /root/.kube/config
kubectl delete pod -n default --all  --kubeconfig /root/.kube/config

kubectl apply -f /root/k8s/ --kubeconfig /root/.kube/config

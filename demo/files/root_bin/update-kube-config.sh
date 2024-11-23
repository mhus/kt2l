#!/bin/bash
#
# kt2l - KT2L (ktool) is a web based tool to manage your kubernetes clusters.
# Copyright © 2024 Mike Hummel (mh@mhus.de)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#


if [ ! -f /home/user/.kube/config-minikube ]; then
  echo "Error: No kubeconfig found in /home/user/.kube/config"
  if [ -f /root/.kube/config-minikube ]; then
    echo "Fallback: Copying /root/.kube/config-minikube to /home/user/.kube/config"
    cp /root/.kube/config-minikube /home/user/.kube/config
  else
    exit 1
  fi
fi
if [ $(grep -c "name: kt2l" /home/user/.kube/config-minikube) != 0 ]; then
  echo "Error: kt2l context already exists in /home/user/.kube/config"
  exit 1
fi

mkdir -p /root/.kube || true
cp -r /home/user/.minikube /root/.minikube || exit 1
cp /home/user/.kube/config-minikube /root/.kube/config || exit 1
cp /home/user/.kube/config-minikube /root/.kube/config-minikube || exit 1
sed -i '' -e 's/\/home\/user\//\/root\//g' /root/.kube/config || true
rm -f /home/user/.kube/config-minikube
rm -rf /home/user/.minikube

# Update these to match your environment
SERVICE_ACCOUNT_NAME=kt2l-service-account
CONTEXT=$(kubectl config current-context)
NAMESPACE=default

NEW_CONTEXT=kt2l
KUBECONFIG_FILE="/home/user/.kube/config"

TOKEN=$(kubectl create token ${SERVICE_ACCOUNT_NAME} -n ${NAMESPACE} --duration=8760h)

# Create dedicated kubeconfig
# Create a full copy
kubectl config view --raw > ${KUBECONFIG_FILE}.full.tmp
# Switch working context to correct context
kubectl --kubeconfig ${KUBECONFIG_FILE}.full.tmp config use-context ${CONTEXT}
# Minify
kubectl --kubeconfig ${KUBECONFIG_FILE}.full.tmp \
  config view --flatten --minify > ${KUBECONFIG_FILE}.tmp
# Rename context
kubectl config --kubeconfig ${KUBECONFIG_FILE}.tmp \
  rename-context ${CONTEXT} ${NEW_CONTEXT}
# Create token user
kubectl config --kubeconfig ${KUBECONFIG_FILE}.tmp \
  set-credentials ${CONTEXT}-${NAMESPACE}-token-user \
  --token ${TOKEN}
# Set context to use token user
kubectl config --kubeconfig ${KUBECONFIG_FILE}.tmp \
  set-context ${NEW_CONTEXT} --user ${CONTEXT}-${NAMESPACE}-token-user
# Set context to correct namespace
kubectl config --kubeconfig ${KUBECONFIG_FILE}.tmp \
  set-context ${NEW_CONTEXT} --namespace ${NAMESPACE}
# Flatten/minify kubeconfig
kubectl config --kubeconfig ${KUBECONFIG_FILE}.tmp \
  view --flatten --minify > ${KUBECONFIG_FILE}
# Remove tmp
rm ${KUBECONFIG_FILE}.full.tmp
rm ${KUBECONFIG_FILE}.tmp
chmod 600 ${KUBECONFIG_FILE}
chown user:user ${KUBECONFIG_FILE}

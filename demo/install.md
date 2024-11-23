
# Server

Image: Ubuntu 24.04
```bash
eval $(ssh-agent)
ssh root@188.245.190.140 -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking no"
```

# Install Basics

```bash
useradd -u 1001 -m -s /bin/bash user
echo "user      ALL = NOPASSWD: ALL" > /etc/sudoers.d/10-install-user
su - user
```

# Docker

## Add Docker's official GPG key:
-- https://docs.docker.com/engine/install/ubuntu/

```bash
sudo apt-get update
sudo apt-get -y install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
```

## Add the repository to Apt sources:

```bash
echo \
"deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
$(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

# Prepare user for docker

```bash
sudo usermod -aG docker user
exit
su - user
```

# Minikube

```bash
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube_latest_amd64.deb
sudo dpkg -i minikube_latest_amd64.deb

minikube start
```

# Tools

```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
```

# Prepare KT2L Config

```bash
mkdir -p ~/config/local

cat << EOF > ~/config/local/users.yaml
allowCreateUsers: true
allowUpdateUsers: true
allowDeleteUsers: true

users:
- name: "admin"
  id: "admin"
  password: "{noop}jKjau86G"
  roles:
  - "READ"
  - "WRITE"
  - "LOCAL"
  - "SETTINGS"
  - "ADMIN"
- name: "user"
  id: "user"
  password: "{noop}hIUYHh72jhb"
  roles:
  - "READ"
  - "WRITE"
  - "SETTINGS"
- name: "viewer"
  id: "viewer"
  password: "{noop}uiJKjb127khj"
  roles:
  - "READ"
EOF

cat << EOF > ~/config/local/login.yaml
autoLogin: false
autoLoginUser: nouser
loginText: Demo
EOF

cat << EOF > ~/config/local/clusters.yaml
defaultCluster: minikube
defaultResourceType: "pods"
defaultNamespace: default
clusterSelector: false
clusters:
- name: minikube
  title: "Cluster"
  enabled: true
  color: blue
EOF
```

# Start Ollama

```bash
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
docker exec -t ollama ollama pull llama3
```

# Start KT2L Server

```bash
docker run -d --restart=always --name kt2l-server \
--network host \
-v "$HOME:/home/user" \
-e CONFIGURATION_DIRECTORY=/home/user/config \
mhus/kt2l-server:snapshot
```

# Firewall

- GitHub
- Docker HUB

```bash
sudo ufw allow 9080
sudo ufw allow ssh
sudo ufw allow out 53
sudo ufw default deny incoming
sudo ufw default allow outgoing

sudo ufw enable
```


sudo ufw default deny outgoing

sudo ufw allow out to 192.168.49.2 # minikube

sudo ufw allow out to 140.82.112.0/20 #	GitHub, Inc.	4,096
sudo ufw allow out to 140.82.112.0/24 #	GitHub, Inc.	256
sudo ufw allow out to 140.82.113.0/24 #	GitHub, Inc.	256
sudo ufw allow out to 140.82.114.0/24 #	GitHub, Inc.	256
sudo ufw allow out to 140.82.115.0/24 #	GitHub, Inc.	256
sudo ufw allow out to 140.82.116.0/24 #	GitHub, Inc.	256
sudo ufw allow out to 140.82.117.0/24 #	GitHub, Inc.	256
sudo ufw allow out to 140.82.120.0/23 #	GitHub, Inc.	512
sudo ufw allow out to 140.82.120.0/24 #	GitHub, Inc.	256
sudo ufw allow out to 140.82.121.0/24 # GitHub, Inc.	256

sudo ufw allow out to 52.44.227.212 # docker hub
sudo ufw allow out to 54.156.140.159 # docker hub
sudo ufw allow out to 44.221.37.199 # docker hub

# Cleanup

```bash
sudo rm /etc/sudoers.d/10-install-user
```

# SSL Proxy

https://github.com/jwilder/docker-letsencrypt-nginx-proxy-companion

```shell
# Update these to match your environment
SERVICE_ACCOUNT_NAME=kt2l-service-account
CONTEXT=$(kubectl config current-context)
NAMESPACE=default

NEW_CONTEXT=kt2l
KUBECONFIG_FILE="/home/user/.kube/config"

SECRET_NAME=$(kubectl get serviceaccount ${SERVICE_ACCOUNT_NAME} \
  --context ${CONTEXT} \
  --namespace ${NAMESPACE} \
  -o jsonpath='{.secrets[0].name}')
TOKEN_DATA=$(kubectl get secret ${SECRET_NAME} \
  --context ${CONTEXT} \
  --namespace ${NAMESPACE} \
  -o jsonpath='{.data.token}')

TOKEN=$(echo ${TOKEN_DATA} | base64 -d)

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

```


# Restart on reboot

```bash
sudo -u user minikube start
/root/update-kubeconfig.sh
docker start ollama
docker start kt2l-server
``` 
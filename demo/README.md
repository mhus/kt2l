
# Prepare local system

```shell
eval "$(ssh-agent -s)"  
ssh-add
```

# Magic with ansible

```shell

# Install all
ansible-playbook -i environments/prod/inventory -b --user root install_demo.yaml -v

# Update kt2l server
ansible-playbook -i environments/prod/inventory -b --user root update_kt2l_server.yaml

# Update k8s cluster
ansible-playbook -i environments/prod/inventory -b --user root update_k8s.yaml

```

# Clean local after server rebuild

```shell
sed -i '' -e '/^demo\.kt2l\.org/d' ~/.ssh/known_hosts
```
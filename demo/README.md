
```shell
eval "$(ssh-agent -s)"  
ssh-add
```

```shell
ansible-playbook -i environments/prod/inventory -b -K --user root install_demo.yml
```

```shell
sed -i '' -e '/^demo\.kt2l\.org/d' ~/.ssh/known_hosts
```
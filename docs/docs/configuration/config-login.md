---
sidebar_position: 10
title: Login Configuration
---

## Login Configuration

The configuration is used to configure the login process and the user interface. 
The configuration is stored in the `config/login.yaml` file.

```yaml
autoLogin: true
autoLoginUser: autologin
autoLoginLocalhostOnly: true
```

`autoLogin` enables automatic login with the user specified in `autoLoginUser`. If
`autoLoginLocalhostOnly` is enabled, the auto login is only possible from localhost.
Other IP sources will be prompted for login.

The user specified in `autoLoginUser` must be a known user. The user must be defined in the `config/users.yaml` file.



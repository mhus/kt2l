---
sidebar_position: 11
title: Users
---

# Users Configuration

The user configuration is used to configure the user specific settings. The configuration is stored in the `config/users.yaml` file.

```yaml
users:
  - name: "admin"
    password: "{generate}"
    roles:
      - "READ"
      - "WRITE"
      - "LOCAL"
      - "SETTINGS"
      - "ADMIN"
```

Under the `users` key you can define the users. The `name` key is the username. The 
`password` key is the password. You can use the `{generate}` keyword to generate a 
random password. It will be printed in the log output. The `roles` key is a list of 
roles the user has. The following default roles are available:

- `READ`: The user can read files and directories.
- `WRITE`: The user can write files and directories.
- `LOCAL`: The user can execute local commands.
- `SETTINGS`: The user can change the user settings.
- `ADMIN`: The user is an admin and can change the configuration.

It's possible to define custom roles also. Use custom roles in the `aaa` configuration
to define the permissions.

The password can also be set with other types

- `{noop}plain`: The password is stored in plain text.
- `{env}ENV_KEY`: The password is stored and loaded from an environment variable.
- `{bcrypt}hash`: The password is stored as a bcrypt hash.

If you use `{env}ENV_KEY` the password is stored in the environment variable `ENV_KEY`. If the
value starts with a wavy bracket `{` it is treated as encoded password. A password can't start 
with a wavy bracket if loaded from environment variable.

For more options see the spring boot configuration 
[Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).

By default, generated passwords will be reported in the log file. If you want to suppress this, you can set the 
environment variable `KT2L_UNSECURE` to `false`.

## User Repository

By default, the users are stored in memory. Initially the users are loaded from the `config/users.yaml` file. This
configuration is used to configure this repository. If another repository is used this configuration is not valid.

Configure access to the user repository with the following parameters:

```yaml
allowCreateUsers: false
allowUpdateUsers: false
allowDeleteUsers: false
```

By default, the access is restricted then it's not possible to use SSO logins. If you want to use SSO logins you
have to set the access to true.

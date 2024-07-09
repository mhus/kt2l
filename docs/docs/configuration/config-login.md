---
sidebar_position: 10
title: Login
---

# Login Configuration

The configuration is used to configure the login process and the user interface. 
The configuration is stored in the `config/login.yaml` file.

```yaml
autoLogin: true
autoLoginUser: autologin
autoLoginLocalhostOnly: true
showLoginHeader: true
loginText:
```

`autoLogin` enables automatic login with the user specified in `autoLoginUser`. If
`autoLoginLocalhostOnly` is enabled, the auto login is only possible from localhost.
Other IP sources will be prompted for login.

The user specified in `autoLoginUser` must be a known user. The user must be defined in the `config/users.yaml` file.

See also the [User Configuration](config-users) for more information about the user configuration.

`showLoginHeader` is a flag to show the login header title.
`loginText` is the text that is shown on the login page. The text is an HTML based text and can break the website.

```yaml
showLoginHeader: false
loginText: |
  <h1>Welcome</h1>
  <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</p>
  <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</p>
```

```yaml
## SSO Configuration

If you run the application as server you can use the single sign-on (SSO) feature to login with OAuth2 providers.
To configure the single sign-on (SSO) you can use the following configuration:

```yaml
autoLogin: false
oauth2Enabled: true
oauth2Providers:
  - id: google
    roleMapping:
        role: [READ]
oauth2Accept:
  - pattern: '.*@gmail.com'
    provider: google
    acceptRoles: ['.*']
    userConfigPreset: google
    defaultRoles: [READ]
```

Disable autologint to show the login page. Enable `oauth2Enabled` to enable the OAuth2 login. With
`oauth2Providers` you can define the OAuth2 providers to use. The `id` is the identifier of the provider
and must be unique. The `roleMapping` maps the roles of the provider to the roles of the user. 
The `oauth2Accept` is a list of email patterns that are accepted for login. The list will be
processes from top to bottom. The `userConfigPreset` is the user configuration preset to use for the user. It
will be created if no user configuration exists. The `defaultRoles` are the roles that are assigned to the user.

The option `provider` is optional and can specify the provider to use for the user. If the provider is not specified
the rule will match every provider.

The `acceptRoles` is a list of roles that are accepted for the user. The roles are regular expressions. If the user
has a role that matches the regular expression the user is accepted.  If the option is not specified the rule will
match every role. If the user has no role and this option is set with a not empty list or patterns the user will not be
accepted.

The `oauth2Providers` configuration is provider specific. The following providers are supported:
- google
- github
- facebook

You also need to set the environment variables for the OAuth2 configuration.

- GOOGLE_CLIENT_ID
- GOOGLE_CLIENT_SECRET
- GITHUB_CLIENT_ID
- GITHUB_CLIENT_SECRET
- FACEBOOK_CLIENT_ID
- FACEBOOK_CLIENT_SECRET

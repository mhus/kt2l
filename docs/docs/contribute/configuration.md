---
sidebar_position: 14
title: Using Configurations
---

# Using Configurations

Configurations are services in KT2L. To use a configuration
simply Autowire the `ConfigurationService` and call the `get` methods.

There are two types of configurations. A single static configuration and user related configurations.
The user configurations are looked up also in the user directories. If the configuration is not protected
the user can overwrite the configuration in its own home directory.

Static configurations are stored in the `config` directory of the KT2L installation.

The user is read from the vaadin UI variable. In some cases this is not available. In this case you need
to remember the SecurityContext calling the static method `SecurityContext.context()` and
encapsulate the code with a try use block.

```java
final var cc = SecurityContext.create();
Thread.startVirtualThread(() -> {
    try (var cce = cc.enter()) {
        // your code here ...
    }
});
```

## Create a custom Configuration services

To create a custom configuration service you need to extend the `AbstractUserRelatedConfig` or `AbstractSingleConfig` base class. Call the
constructor with the basename of the configuration file and use `config()` to get the base node of the configuration each time you access a
configuration parameter. The `config()` method is getting the configuration from current the user each time you call it. The method has caching
implemented so you can call it as often as you like.

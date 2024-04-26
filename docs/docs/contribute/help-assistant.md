---
sidebar_position: 3
title: Help Assistant Action
---

# Help Assistant Action

To provide an additional help assistant you can implement the `HelpAction` interface. The `HelpAction` interface 
provides a method method `canHandle` to check if the action can handle the help section configured
in the `help.yaml`.

```java
public interface HelpAction {

    boolean canHandle(HelpConfiguration.HelpLink link);

    void execute(Core core, HelpConfiguration.HelpLink link);

    Icon getIcon(HelpConfiguration.HelpLink link);
}
```

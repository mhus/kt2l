---
sidebar_position: 2
title: Help Configuration
---

# Help Configuration

The help configuration is used to configure the help system. The configuration is stored in the `config/help.yaml` file.

Example:
```yaml
windowWidth: 400px
enabled: true
contexts:
  default:
    - name: Chat Agent
      action: ai
    - name: Documentation
      action: docs
      document: index
    - name: Kubernetes
      action: link
      href: https://kubernetes.io/docs/home/
```

The property `windowWidth` defines the width of the help window. The property `enabled` defines if the help system 
is enabled. The property `contexts` defines the help entries. 

Each entry has a `name` and an `action`. It presents an entry in the help menu. If the `action` is not available
it will be ignored.

The `action` can be one of the following:
- `ai` - open the chat agent in the help system
- `docs` - open a document in the help system
- `link` - open a link in the system browser

The `ai` supports the following properties:
- `model` - the model to use
- `prompt` - the initial prompt



---
sidebar_position: 10
title: AI Integration
---

# AI Integration

To enable AI copy the ai configuration file to the local 
directory and set the `enabled` flag to `true`.

```yaml
enabled: true
ollamaUrl: http://localhost:11434
openAiKey: xxxx
prompts:
  translate:
    model: ollama/yi
    template: |
      Translate the following text to {{language}}:
      
      {{content}}
  resource:
    model: ollama/codellama
    template: |
      Do you see problems in the following kubernetes resource?
      
      {{content}}
```

You can optimize the prompt templates for the models you use. 
The `{{content}}` placeholder will be replaced with the content 
of the current klubernetes resource. The `{{language}}` placeholder 
will be replaced with the language requested language.

You can use the following models:

* `ollama/*`: all models from the ollama service, you have to install the models manually before. Set ollamaUrl to the correct url.
* `openai/*`: all models from the openai service, you have to set the `openAiKey` in the configuration file.

You can add options for the model like this `ollama/codellama/temperature=0.3&timeout=60`.

* `temperature`: The temperature of the model, a higher temperature will result in more creative answers.
* `timeout`: The timeout in seconds for the model.
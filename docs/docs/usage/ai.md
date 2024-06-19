---
sidebar_position: 10
title: AI Panel
---
# AI

AI will analyze each selected resource. The AI panel will show the results of the analysis.
In the Question text field, you can ask a question about the resources. Use the placeholder
`{{content}}` to refer to the content of the resource. The AI will try to answer the question
based on the content of the resource.

If you do not provide the `{{content}}` placeholder, it will automatically be added to the question.
To suppress the automatic addition of the `{{content}}` placeholder, start the question with a 
hash `#` character.

This is not a chatbot. The AI will not have a memory of previous questions or answers. 
It will only analyze the content of the resource.

Use `Control+Enter` in the question text field or the "Ask" button to ask the question.

## Modes

The AI panel has the following modes:

- resources: The AI will analyze each selected resource.
- cumulative: The AI will analyze all selected resources together.
- text: The AI will analyze the text in the text field.

## Commands

If a question starts with a slash `/` character, it is interpreted as a command. The following commands are available:

- `/mode <mode>` - Set the mode of the AI panel. The mode can be `resources`, `cumulative`, or `text`.
- `/model <model>` - Set the model of the AI panel. For example `auto:coding`, `ollama:codellama` or `openai:gpt-3.5-turbo`.
- `/language <language>` - Set the language of the AI panel. For example `german` or `italian`.

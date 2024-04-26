---
sidebar_position: 2
title: Documentation
---

# Documentation

This documentation is intended to help you understand the structure of the project and how to contribute to it.

The documentation is written in markdown and is located in the `docs/docs` folder. The documentation is structured in 
a way that it can be easily navigated. The sidebar is automatically generated from the markdown files in the `docs`.
The system `docusaurus` is used to generate the documentation.

## Guidelines for documentation

- The documentation must be written in English.
- The documentation should be structured in a way that it is easy to navigate.
- The documentation must be written in markdown. 
- Write short and concise sentences.
- Show examples if necessary.

## How to contribute to the documentation

If you want to contribute to the documentation, you can simply create a new markdown file in the `docs/docs` folder or 
subfolder.

To try the documentation locally, you can run the following command:

```bash
cd docs
npm run start
```

## Application Help Pages

Additional to the documentation, the application has help pages that are displayed in the application. These help pages
are located in the `tk2l-core/docs` folder. The help pages are written in markdown and are structured as a flat list of
files. The files are referenced in the `help.yaml` configuration file. Both the help pages and the documentation are
available in the application. It will be generated automatically when the application is built.

## Screenshots

Screenshots are generated automatically in the release workflow while executing the integration tests. The screenshots 
are located in the `docs/screenshots` and must be copied into `docs/docs/screenshots` after review. 

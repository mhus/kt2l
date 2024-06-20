---
sidebar_position: 13
title: Snippets
---

# Snippets

Snippets are managed in the project [kt2l-snippet](https://github.com/mhus/kt2l-snippets). The snippets are used
in the Help Snippets feature of the kt2l. For each category there are a separate directory in the project.

Snippets are written in markdown format with a strict structure.

```markdown
# Scale 

Scale Deployment 
or ReplicaSet.

\```yaml
[{"op":"replace","path":"/spec/replicas","value":"5"}]
\```

keywords
```

The first line is the title of the snippet. The second block is the description of the snippet. The third line is the code block with the snippet.
The last line is a list of keywords. The keywords are used to find the snippet in the search.

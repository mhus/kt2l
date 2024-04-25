---
sidebar_position: 1
title: Contribute
---

# Contribute

## General guidelines
- If you want to contribute a bug fix or a new feature that isn't listed in the
  [issues](https://github.com/mhus/kt2l/issues) yet, please open a new issue for
  it and link it to your PR.
- A lot of [Google's Best Practices for Java Libraries](https://jlbp.dev/) apply to this project as well.
- Keep the code compatible with the latest LTE Java version (Java 21 at the moment).
- Avoid adding new dependencies as much as possible. If absolutely necessary, try to use the same libraries
  which are already used in the project.
- Write unit and/or integration tests for your code.
- Avoid making breaking changes. Always keep backward compatibility in mind. Specially the described behavior
  in this documentation should not change.
- Follow existing naming conventions.
- Follow existing code style present in the project.
- Large features should be discussed with maintainers before implementation. Please ping @mhus in the
  comments on the issue.

## Opening an issue
- Please fill in all sections of the issue template.

## Opening a PR
- Link an [issue](https://github.com/mhus/kt2l/issues) to your PR if possible only small changes can be
  accepted without issue. In this case describe your changes in the PR.
- Fill in all the sections of the PR template.
- Make sure you've added tests if possible.
- Make sure you've added documentation where required.
- For new big features, make sure you've added integration tests and documentation.
- Please make it easier to review your PR:
    - Keep changes as small as possible.
    - Do not combine refactoring with changes in a single PR.
    - Avoid reformatting existing code.
- Provide PR in separate branches. Do not mix multiple PRs in a single branch.

## Guidelines for new features
- Use the existing entry points described in this documentation for new features. Do not add new
  entry points unless absolutely necessary.
- Try to enhance existing features instead of adding new ones.
- New features should be optional and should not change the behavior of existing features.
- It should be possible to integrate new features into existing code without changing the existing code.
- New features should be well documented.
- New features should be tested.

## Guidelines for bug fixes
- Bug fixes should not change the behavior of existing features.
- Bug fixes should have an associated issue or a detailed description of the bug in the PR.

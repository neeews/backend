--- 
name: commit
description: Analyzes Git changes to separate commits by function and purpose, and applies Conventional Commit rules to maintain a clean Git history.
---

# Commit Manager

## Goal

Create clean, maintainable Git history by grouping changes into logical commits.

## Instructions

### Analyze changes first

Always inspect changes before creating commits.

```bash
git status
git diff
git diff --staged
```

### Split commits by purpose

If multiple unrelated changes exist, create separate commits.

Examples:

Good:

* Add login API
* Update README

→ Two commits

Bad:

* Add login API
* Update README
* Change Docker configuration

→ One commit

### Conventional Commits

Use:

```text
feat: add login endpoint
refactor: extract authentication service
chore: update dependencies
```

### Never

* Create giant commits
* Mix frontend and backend changes
* Mix feature work and refactoring
* Commit node_modules
* Commit .env
* Commit build artifacts

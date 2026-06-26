# Working with Git in Subdirectories

This guide explains how to ensure Git only commits files from a specific directory and avoids including files from parent or sibling directories.

## Important: Git repository structure

Git is **repository-wide**. Even if you're in a subdirectory like `elasticsearch-demo-app/`, you're still working with the entire `/Users/A108706402/workspaces/demoElasticSearch` repository.

```
/Users/A108706402/workspaces/demoElasticSearch/          ← Git repo root
├── elasticsearch-demo-app/                              ← You are here
├── log-producer-app/
├── docker/
├── scripts/
└── .git/
```

When you run `git add` or `git commit`, Git considers the **entire repository**, not just your current directory.

## Option 1: Add only files from current directory

Use `git add` with a path specifier:

```bash
cd elasticsearch-demo-app
git add .                    # Add all changes in this directory only
git commit -m "Update demo app"
```

This will **only stage** files in `elasticsearch-demo-app/` and its subdirectories, **not** files from parent directories like `docker/`, `scripts/`, or `log-producer-app/`.

## Option 2: Exclude parent directories with git status filter

See what files Git would commit from current directory:

```bash
cd elasticsearch-demo-app
git status --porcelain       # Shows all changed files in repo
git diff --name-only HEAD   # Shows all changed files from current dir
```

Then add only what you want:

```bash
git add src/                 # Add entire src/ directory
git add pom.xml              # Add specific file
git commit -m "Changes to demo app"
```

## Option 3: Use git add interactively (for selective staging)

```bash
git add --interactive        # or: git add -p
```

This lets you choose which hunks/files to stage, one at a time.

## Option 4: Check which files will be committed

Before committing, verify you're only adding the right files:

```bash
git diff --cached --name-only      # Show staged files
git diff --cached --name-status    # Show staged files with status (A=added, M=modified, D=deleted)
```

Example:
```bash
cd elasticsearch-demo-app
git add .
git diff --cached --name-status
# Output should only show:
# M  elasticsearch-demo-app/pom.xml
# M  elasticsearch-demo-app/src/main/java/...
# NOT files from docker/, scripts/, or log-producer-app/
```

## Option 5: Create separate git repositories (if you want true isolation)

If you want `elasticsearch-demo-app/` to be completely independent:

```bash
cd elasticsearch-demo-app
git init                     # Create a new repo in this directory
git add .
git commit -m "Initial commit"
```

⚠️ **Warning**: This breaks the link with the parent repo. Only do this if you want truly separate repositories.

## Option 6: Use git sparse-checkout (advanced)

For very large repos, you can use sparse-checkout to only work with specific directories:

```bash
cd /Users/A108706402/workspaces
git sparse-checkout init
git sparse-checkout set demoElasticSearch/elasticsearch-demo-app
```

This makes Git ignore files outside the specified paths.

## Recommended workflow for this repository

Since this is a **multi-module Maven project**, use this approach:

### When committing changes to the main demo app:

```bash
# From anywhere in the repo
cd elasticsearch-demo-app

# Stage only files from this directory
git add .

# Verify only your files are staged
git diff --cached --name-status

# Commit
git commit -m "feat: add OpenAPI integration to elasticsearch-demo-app"
```

### When committing changes to the log producer app:

```bash
cd log-producer-app
git add .
git diff --cached --name-status
git commit -m "feat: add OpenAPI integration to log-producer-app"
```

### When committing README or multi-module changes:

```bash
# Go to repo root
cd /Users/A108706402/workspaces/demoElasticSearch

# Add specific files
git add README.md LOGSTASH_PIPELINE_GUIDE.md
git add elasticsearch-demo-app/pom.xml log-producer-app/pom.xml

# Verify
git diff --cached --name-status

# Commit
git commit -m "docs: add Logstash guide and OpenAPI integration"
```

## Common mistakes to avoid

### ❌ DON'T: Stage everything from root

```bash
cd /Users/A108706402/workspaces/demoElasticSearch
git add .                    # This adds EVERYTHING in the repo!
```

### ✅ DO: Stage from your working directory

```bash
cd elasticsearch-demo-app
git add .                    # Only adds elasticsearch-demo-app/* files
```

### ✅ Or be explicit:

```bash
git add elasticsearch-demo-app/pom.xml elasticsearch-demo-app/src/
```

## Double-check before pushing

Always review your commits before pushing:

```bash
git log --oneline -5         # See last 5 commits
git show HEAD --stat         # See what's in the current commit
git diff HEAD~1..HEAD        # See actual changes in current commit
```

Example output showing only demo-app changes:
```
 elasticsearch-demo-app/pom.xml                           |   5 +
 elasticsearch-demo-app/src/main/java/.../DocumentController.java  |  10 ++
```

## Summary

- **Use `git add .` from your current directory** — it only stages files in that directory.
- **Use `git diff --cached` to verify** — ensure you're only committing what you intend.
- **Use absolute paths for clarity** — `git add elasticsearch-demo-app/src/` is safer than `git add .`.
- **Never run `git add .` from the repo root** unless you want to commit everything.

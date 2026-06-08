# AGENT_CONTRIBUTING.md

This file defines the contribution workflow for AI coding agents working on **FiloraFS-Lite**.

The existing `CONTRIBUTING.md` remains the human contributor guide. This file is only for AI-assisted coding workflows.

---

## Purpose

AI agents should help improve FiloraFS-Lite while protecting the repo's core value:

```txt
A clean, minimal Spring Boot file upload API starter.
```

Agents must avoid unnecessary complexity, protect file-handling security, and keep the project easy to understand.

---

## Before Starting Work

An AI agent must first identify the change type:

```txt
[ ] Bug fix
[ ] Security fix
[ ] Documentation update
[ ] API enhancement
[ ] Test improvement
[ ] Refactor
[ ] Configuration change
```

Then read the relevant files.

Minimum reading set for most changes:

```txt
readme.md
SECURITY.md
AGENTS.md
ARCHITECTURE.md
AI_RULES.md
pom.xml
src/main/resources/application.properties
src/main/java/com/file/controller/FileController.java
src/main/java/com/file/services/FileService.java
src/main/java/com/file/filter/ApiKeyFilter.java
```

For test changes, also read:

```txt
src/test/java/com/file/FileUploadApiApplicationTests.java
```

For Postman/API docs changes, also read:

```txt
FiloraFS-Lite.postman_collection.json
```

---

## Branch Naming

Use clear branch names.

Examples:

```txt
agent/docs-ai-guidelines
agent/fix-path-traversal
agent/improve-file-upload-validation
agent/add-file-service-tests
agent/refactor-error-handling
```

Avoid vague names:

```txt
fix
updates
agent-changes
misc
```

---

## Commit Style

Use meaningful commit titles.

Recommended format:

```txt
<type>: <short summary>
```

Examples:

```txt
docs: add AI coding guidelines
fix: prevent path traversal in file access
test: add file service upload validation tests
refactor: return typed file metadata response
security: reject unsafe filenames in file APIs
```

Avoid:

```txt
fix stuff
changes
update files
ai commit
```

---

## Change Scope Rules

Each AI contribution should be small and focused.

Good scope:

```txt
Improve path traversal protection in FileService and add tests.
```

Bad scope:

```txt
Refactor entire app, add database, add JWT, add S3, rewrite README, and change all endpoints.
```

If multiple unrelated issues are found, report them separately instead of bundling everything into one change.

---

## File-Specific Contribution Guidance

### `FileController.java`

Allowed changes:

- Improve response status handling.
- Return `ResponseEntity` where useful.
- Remove broad exception swallowing.
- Improve stream handling.
- Keep route compatibility.

Avoid:

- Moving storage logic into controller.
- Changing endpoint paths without explicit requirement.
- Returning stack traces.

---

### `FileService.java`

Allowed changes:

- Improve safe path resolution.
- Improve upload validation.
- Add typed generics.
- Improve metadata handling.
- Extract helper methods.
- Use `Path` and `Files` APIs.

Avoid:

- Trusting raw filename input.
- Writing outside `upload.path`.
- Deleting directories.
- Adding database persistence unless explicitly requested.
- Adding cloud storage unless explicitly requested.

---

### `ApiKeyFilter.java`

Allowed changes:

- Improve error response format.
- Make header name configurable if required.
- Improve API-key configuration safety.
- Add tests for missing/invalid/valid key behavior.

Avoid:

- Disabling API-key checks.
- Logging full API keys.
- Making all routes public unintentionally.

---

### `application.properties`

Allowed changes:

- Improve placeholders.
- Add environment-variable support.
- Document config values.

Avoid:

- Committing real upload paths.
- Committing real API keys.
- Changing default port without reason.

---

### `readme.md`

Allowed changes:

- Improve setup steps.
- Document required config.
- Document API header usage.
- Add curl examples.
- Add security warnings.

Avoid:

- Overpromising production readiness.
- Claiming features that do not exist.
- Removing BuildBaseKit attribution.

---

### `FiloraFS-Lite.postman_collection.json`

Allowed changes:

- Replace real-looking API keys with placeholders.
- Keep endpoint paths aligned with code.
- Update descriptions when behavior changes.

Avoid:

- Adding personal local file paths.
- Adding real secrets.
- Leaving examples inconsistent with README.

---

## Required Validation

Before submitting, run where possible:

```bash
mvn test
```

For larger changes, also run:

```bash
mvn clean package
```

If tests cannot be run, state exactly why.

Do not write:

```txt
Tests passed
```

unless the command was actually executed and completed successfully.

---

## Security Validation

For any file-handling change, manually verify:

```txt
[ ] Upload cannot save outside upload.path.
[ ] Stream cannot read outside upload.path.
[ ] Delete cannot remove files outside upload.path.
[ ] Metadata cannot inspect files outside upload.path.
[ ] Unsupported file types are rejected.
[ ] Missing/wrong API key is rejected.
[ ] Error responses do not leak internal paths.
[ ] No real secrets were committed.
```

For path traversal, test values like:

```txt
../test.txt
..\test.txt
../../application.properties
/etc/passwd
C:\Windows\win.ini
```

---

## Pull Request Description Template

Use this structure for AI-generated pull requests:

```md
## Summary

- What changed?
- Why was it needed?

## Changed Files

- `path/to/file`
- `path/to/file`

## Behavior Changes

- List endpoint, response, config, or security behavior changes.
- Write `None` if there are no behavior changes.

## Security Notes

- Mention upload, path traversal, API key, MIME validation, or secret-handling impact.

## Validation

- [ ] `mvn test`
- [ ] `mvn clean package`

If not run, explain why.

## Backward Compatibility

- Does this preserve current API behavior?
- If not, what changed?
```

---

## Review Checklist for AI Agents

Before final response or PR submission:

```txt
[ ] I read the relevant existing code before editing.
[ ] I did not overwrite human `CONTRIBUTING.md`.
[ ] I kept the change focused.
[ ] I did not add unrelated features.
[ ] I did not commit real secrets.
[ ] I considered path traversal.
[ ] I considered MIME/file validation.
[ ] I considered API-key behavior.
[ ] I updated docs if behavior/config changed.
[ ] I ran tests or clearly stated why not.
[ ] I summarized security impact honestly.
```

---

## Recommended Issue Breakdown

When an AI agent finds multiple improvements, split them like this:

1. Security: safe path resolution.
2. Security: stronger upload validation.
3. API: typed response DTOs.
4. API: controlled error handling.
5. Tests: service tests with temporary directory.
6. Docs: setup and API examples.
7. Postman: placeholder API key and aligned examples.

Do not collapse all of these into one giant commit unless explicitly asked.

---

## Agent Communication Standard

When reporting work, be direct:

Good:

```txt
Implemented safe path resolution in FileService and added tests for traversal attempts. `mvn test` passed.
```

Bad:

```txt
Everything is production ready now.
```

Agents must not overclaim. FiloraFS-Lite is a starter project; production readiness depends on deployment configuration, security review, storage strategy, monitoring, and operational controls.

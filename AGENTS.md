# AGENTS.md

This file is the operating manual for AI coding agents working on **FiloraFS-Lite**.

FiloraFS-Lite is a lightweight Spring Boot file-upload and file-management API boilerplate. It exposes REST APIs for uploading files, streaming files, deleting files, listing stored files, and reading file metadata. The current implementation uses local filesystem storage and API-key-based request filtering.

AI agents must treat this repository as a minimal backend starter. Do not turn it into a large enterprise platform unless explicitly asked.

---

## Required Reading Order

Before making any code changes, read these files in order:

1. `readme.md`
2. `SECURITY.md`
3. `ARCHITECTURE.md`
4. `AI_RULES.md`
5. `AGENT_CONTRIBUTING.md`
6. Existing human guide: `CONTRIBUTING.md`
7. `pom.xml`
8. `src/main/resources/application.properties`
9. `src/main/java/com/file/controller/FileController.java`
10. `src/main/java/com/file/services/FileService.java`
11. `src/main/java/com/file/filter/ApiKeyFilter.java`
12. `src/main/java/com/file/dtos/FileStream.java`

Do not assume behavior from file names alone. Verify implementation before editing.

---

## Project Snapshot

- Language: Java 25
- Framework: Spring Boot 4.1.1
- Build tool: Maven
- Main package: `com.file`
- Application class: `FileUploadApiApplication`
- Default server port: `8000`
- Storage model: local filesystem directory configured by `filorafs.storage-path`
- API protection: `X-API-KEY` header checked by `ApiKeyFilter`
- Database: none
- Current endpoints are under `/file`

---

## Source Layout

```txt
src/main/java/com/file/
├── FileUploadApiApplication.java
├── controller/
│   └── FileController.java
├── dtos/
│   └── FileStream.java
├── filter/
│   └── ApiKeyFilter.java
└── services/
    └── FileService.java

src/main/resources/
├── application.properties
└── static/
    └── api-test/
        ├── index.html
        ├── styles.css
        └── app.js

src/test/java/com/file/
└── FileUploadApiApplicationTests.java
```

---

## Agent Mission

When working on this repo, optimize for:

1. Secure file handling.
2. Simple, readable Spring Boot code.
3. Stable REST API contracts.
4. Minimal dependencies.
5. Boilerplate usefulness for real projects.
6. Clear documentation for setup and extension.

Do not over-engineer the codebase. This is intentionally lightweight.

---

## Non-Negotiable Rules

### 1. Do not overwrite human documentation

`CONTRIBUTING.md` is for human contributors. Do not replace it with AI-agent instructions.

Use these AI-specific files instead:

```txt
AGENTS.md
ARCHITECTURE.md
AI_RULES.md
AGENT_CONTRIBUTING.md
```

### 2. Do not commit secrets

Never commit a real API key in:

- `application.properties`
- Postman collections
- README examples
- test files
- shell scripts
- generated docs

Use placeholders such as:

```properties
filorafs.api-key=${FILORAFS_API_KEY}
filorafs.storage-path=${FILORAFS_STORAGE_PATH}
```

or clearly fake values such as:

```txt
<your-api-key>
```

### 3. Keep API-key behavior explicit

The project uses `X-API-KEY` for access control. Any change to authentication must be clearly documented and must not silently break existing clients.

### 4. File security must be treated as high risk

Any code touching upload, download, delete, list, or metadata logic must consider:

- path traversal
- unsafe filename usage
- MIME spoofing
- oversized files
- empty files
- duplicate names
- unknown extensions
- missing upload directory
- filesystem permission failures
- serving files outside the upload directory

### 5. Preserve the minimal architecture

Do not introduce database persistence, JWT auth, S3, Redis, queues, or heavy abstractions unless requested.

For this Lite repository, local filesystem storage is the baseline.

---

## Current API Contract

Base path:

```txt
/file
```

Endpoints:

```txt
POST   /file                 Upload a file using multipart field `file`
GET    /file/{filename}      Stream a stored file
DELETE /file/{filename}      Delete a stored file
GET    /file/list            List stored filenames
GET    /file/info/{filename} Read metadata for one file
```

Required request header:

```txt
X-API-KEY: <configured-api-key>
```

When modifying these endpoints, preserve backward compatibility unless the task explicitly requires a breaking change.

---

## Build and Test Commands

Use Maven commands from the repository root:

```bash
mvn test
mvn clean package
mvn spring-boot:run
```

If `application.properties` contains placeholder values, configure valid local values before running integration-style tests.

---

## Configuration Rules

Current configurable properties:

```properties
spring.application.name=FiloraFS-Lite
server.port=8000
spring.config.import=optional:file:.env[.properties]
filorafs.storage-path=${FILORAFS_STORAGE_PATH:./uploads}
filorafs.api-key=${FILORAFS_API_KEY:filorafs-local-dev-key}
spring.servlet.multipart.max-file-size=${FILORAFS_MAX_FILE_SIZE:10MB}
spring.servlet.multipart.max-request-size=${FILORAFS_MAX_REQUEST_SIZE:10MB}
```

When improving configuration:

- Prefer environment-variable overrides.
- Keep safe defaults where possible.
- Do not hardcode absolute developer-machine paths.
- Do not commit real keys.
- Document new properties in `readme.md` or a dedicated configuration section.

---

## Coding Style

Follow the current Spring Boot style, but improve safety and readability when touching files.

Preferred direction:

- Use constructor injection.
- Prefer `private final` dependencies where possible.
- Use generic types such as `List<String>` and `Map<String, Object>` instead of raw `List` and raw `Map`.
- Use `Path` and `Files` APIs for filesystem work when changing file logic.
- Return meaningful HTTP responses from controllers.
- Avoid `printStackTrace()` in production paths.
- Avoid swallowing exceptions and returning null.

---

## High-Risk Areas

Be extra careful with:

1. `FileService#getFullPath`
   - Current path handling is string-based.
   - Any agent touching this must prevent path traversal.

2. `FileService#saveFile`
   - Current validation relies on client-provided content type.
   - Improve carefully if asked, but do not introduce heavy libraries without approval.

3. `FileService#getFile`
   - Current fallback behavior uses `default.png` if a requested file does not exist.
   - Preserve or explicitly document any change to fallback behavior.

4. `FileController#getFile`
   - Current exception handling catches broad exceptions and prints stack traces.
   - Prefer proper HTTP status responses when refactoring.

5. `ApiKeyFilter`
   - Applies globally as a Spring component.
   - Allows `OPTIONS` requests through.
   - Ignores `/api-test` endpoints via `shouldNotFilter` so developers can use the UI.
   - Uses constant-time comparison for API key equality.

---

## Testing Expectations

When changing behavior, add or update tests.

Minimum expected coverage for meaningful changes:

- Context load test still passes.
- Upload success case.
- Upload rejection for unsupported MIME type.
- API key missing or invalid returns unauthorized.
- Stream existing file returns correct content type.
- Delete existing and missing file behavior.
- Metadata endpoint behavior for existing and missing files.
- Path traversal attempts are blocked if path logic is touched.

Use temporary directories for filesystem tests. Do not write tests that depend on a developer's local machine path.

---

## Good Agent Behavior

Before coding:

- Identify the specific files affected.
- Read the relevant implementation.
- Check whether the change touches API behavior, security, config, or file I/O.
- State assumptions clearly in the PR or summary.

While coding:

- Make the smallest safe change.
- Keep naming consistent.
- Avoid broad rewrites.
- Do not add dependencies unless they are necessary and justified.

After coding:

- Run tests where possible.
- Summarize changed files.
- Mention any behavior changes.
- Mention any security implications.

---

## What Not To Do

Do not:

- Replace the project with a different architecture.
- Add a database without being asked.
- Add authentication frameworks without being asked.
- Add cloud storage without being asked.
- Store files in `src/main/resources`.
- Expose upload directories as static public folders by default.
- Trust file extensions or MIME types blindly.
- Use user-supplied filenames directly as storage names.
- Return stack traces to API clients.
- Break the provided Postman flow without documenting the change.

---

## Final Check Before Submitting Changes

Before finalizing any AI-generated change, verify:

```txt
[ ] Existing endpoints still behave as documented, or breaking changes are clearly documented.
[ ] No real secrets were added.
[ ] Upload path is configurable.
[ ] File path handling does not allow traversal outside filorafs.storage-path.
[ ] Unsupported file types are rejected.
[ ] Missing/invalid API key is rejected.
[ ] Tests were added or updated for changed behavior.
[ ] README or docs were updated if setup/API behavior changed.
```

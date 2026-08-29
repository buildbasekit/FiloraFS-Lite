# AI_RULES.md

These are strict rules for AI-assisted coding in **FiloraFS-Lite**.

The goal is to help AI agents make safe, focused, production-minded changes without damaging the simplicity of the project.

---

## Prime Directive

Make the smallest safe change that solves the requested problem.

FiloraFS-Lite is a lightweight Spring Boot file upload API starter. Do not convert it into a full platform unless explicitly instructed.

---

## Repository-Specific Facts

AI agents must treat these facts as the current baseline:

```txt
Main package:        com.file
Main class:          FileUploadApiApplication
Framework:           Spring Boot 4.1.1
Build tool:          Maven
Java version:        25
Default port:        8000
Storage:             local filesystem
Upload property:     filorafs.storage-path
API key property:    filorafs.api-key
API key header:      X-API-KEY
Base API route:      /file
Database:            none
```

---

## Hard Rules

### 1. Do not overwrite `CONTRIBUTING.md`

`CONTRIBUTING.md` is the human contributor guide.

AI-specific contribution rules must live in:

```txt
AGENT_CONTRIBUTING.md
```

### 2. Do not commit secrets

Never add real credentials, tokens, API keys, or machine-specific paths.

Bad:

```properties
filorafs.api-key=my-real-production-key
filorafs.storage-path=C:/Users/Amit/Desktop/uploads
```

Good:

```properties
filorafs.api-key=${FILORAFS_API_KEY}
filorafs.storage-path=${FILORAFS_STORAGE_PATH}
```

or use clearly fake placeholder values in documentation examples.

### 3. Do not trust user input

Treat all of these as untrusted:

- Uploaded file contents.
- `MultipartFile#getOriginalFilename()`.
- `MultipartFile#getContentType()`.
- `{filename}` path variables.
- HTTP headers.
- Postman examples.

### 4. Do not allow path traversal

Any code that resolves a filename to disk must prevent values like:

```txt
../secret.txt
..\secret.txt
/etc/passwd
C:\Windows\system32\drivers\etc\hosts
folder/../../file.txt
```

Preferred strategy:

```java
Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
Path target = root.resolve(filename).normalize();
if (!target.startsWith(root)) {
    throw new InvalidFilenameException("Invalid filename");
}
```

Do not rely only on string concatenation.

### 5. Do not use original filename as stored filename

The upload flow must continue to use server-generated names such as UUID-based names.

Original filename may be used only for deriving a safe extension, and even that must be handled carefully.

### 6. Do not silently change public endpoints

Current endpoint contract:

```txt
POST   /file
GET    /file/{filename}
DELETE /file/{filename}
GET    /file/list
GET    /file/info/{filename}
```

Breaking route or response changes require explicit documentation.

### 7. Do not add heavy dependencies casually

Avoid adding libraries unless the task clearly requires them.

Before adding a dependency, ask:

- Can this be solved with JDK/Spring APIs?
- Does this dependency materially improve safety or maintainability?
- Is the dependency appropriate for a lightweight starter?

### 8. Do not swallow exceptions

Avoid this pattern:

```java
try {
    // work
} catch (Exception ex) {
    ex.printStackTrace();
    return null;
}
```

Prefer explicit exceptions and controlled API responses.

### 9. Do not return stack traces to clients

Client-facing errors should be short and safe.

Good:

```json
{
  "message": "File not found"
}
```

Bad:

```txt
java.io.FileNotFoundException: C:\server\internal\path\secret.txt
```

### 10. Preserve API-key protection

Do not bypass `ApiKeyFilter` unless creating an explicitly public endpoint and documenting why.

The default stance is: all file APIs (`/file/**`) require `X-API-KEY`. The `/api-test` resources are allowed through `shouldNotFilter`.

---

## File Upload Rules

When modifying upload logic:

- Validate file is present.
- Reject empty files if appropriate.
- Enforce allowed types.
- Keep multipart limits configured.
- Generate server-side filename.
- Save only inside configured upload directory.
- Handle missing upload directory safely.
- Return a stable response.

Current allowed types:

```txt
image/png
image/jpeg
application/pdf
image/webp
```

Do not expand allowed types without clear use-case justification.

---

## File Streaming Rules

When modifying download/stream logic:

- Resolve the requested filename safely.
- Never stream files outside `filorafs.storage-path`.
- Set correct content type.
- Close streams safely.
- Return 404 or controlled fallback for missing files.
- Do not expose internal filesystem paths.

- Return 404 for missing files.

---

## File Delete Rules

When modifying delete logic:

- Resolve filename safely.
- Delete only files under `filorafs.storage-path`.
- Do not delete directories.
- Return clear result/status.
- Consider missing file behavior deliberately.

- Consider missing file behavior deliberately.

---

## File List Rules

When modifying list logic:

- List only regular files.
- Do not recursively expose directories unless asked.
- Consider pagination if directory size may be large.
- Do not expose absolute paths.
- Return filenames or sanitized metadata only.

---

## Metadata Rules

When modifying metadata logic:

- Return safe metadata only.
- Do not expose absolute local paths.
- Handle missing files clearly.
- Prefer typed DTOs over raw `Map`.

Acceptable metadata:

```txt
name
size
mimeType
lastModified
```

Avoid leaking:

```txt
absolutePath
serverUsername
internalStorageRoot
system-specific details
```

---

## Configuration Rules

When editing `application.properties`:

- Keep placeholders for secrets.
- Keep default port unless requested.
- Do not hardcode local machine paths.
- Keep multipart limits explicit.
- Document new properties.

Preferred production-friendly style:

```properties
filorafs.storage-path=${FILORAFS_STORAGE_PATH:./uploads}
filorafs.api-key=${FILORAFS_API_KEY:filorafs-local-dev-key}
```

If using default fallback values, make sure documentation warns users to override them in production.

---

## Testing Rules

Any non-trivial behavior change must include tests.

Test types to prefer:

- Unit tests for path resolution and validation.
- MVC/filter tests for API-key behavior.
- Service tests using temporary directories.
- Controller tests for status codes and response shapes.

Use temporary folders. Do not depend on local absolute paths.

Important cases:

```txt
[ ] Missing API key returns 401.
[ ] Wrong API key returns 401.
[ ] Valid API key allows request.
[ ] Unsupported MIME type is rejected.
[ ] Upload returns generated filename.
[ ] Path traversal filename is rejected.
[ ] Missing file behavior is controlled.
[ ] Delete cannot escape filorafs.storage-path.
[ ] Metadata does not expose absolute path.
```

---

## API Response Rules

Prefer predictable response objects for new or refactored endpoints.

Examples:

```json
{
  "filename": "uuid.png"
}
```

```json
{
  "name": "uuid.png",
  "sizeKB": 120,
  "mimeType": "image/png",
  "lastModified": "2026-06-08T10:00:00Z"
}
```

```json
{
  "message": "File type not allowed"
}
```

Do not introduce inconsistent response shapes without reason.

---

## Java Style Rules

Follow these conventions:

- Use Java 25-compatible code.
- Use clear method names.
- Keep controller thin.
- Keep filesystem logic inside service or a dedicated storage helper.
- Prefer `Path`/`Files` over string path concatenation for new path logic.
- Use generics: `List<String>`, `Map<String, Object>`.
- Prefer `final` for injected dependencies.
- Avoid static mutable state.
- Avoid unnecessary comments that repeat obvious code.

---

## Spring Boot Rules

- Keep Spring components focused.
- Use constructor injection.
- Avoid field injection for new dependencies.
- Do not create manual singletons.
- Use `@Value` only for simple configuration, or introduce `@ConfigurationProperties` if configuration grows.
- Do not add Spring Security unless explicitly requested; current repo uses a simple filter.

---

## Documentation Rules

Update docs when changing:

- endpoint path
- request headers
- response shape
- allowed file types
- upload size limits
- configuration properties
- security behavior
- run instructions

Do not leave README or Postman instructions stale after API changes.

---

## Refactoring Rules

Allowed refactors:

- Improve formatting.
- Add generics.
- Replace unsafe path handling.
- Extract small helper methods/classes.
- Add typed DTOs.
- Add controlled exception handling.

Avoid refactors that:

- Change endpoint behavior unintentionally.
- Add broad package structures for no benefit.
- Mix unrelated changes.
- Reformat the entire repo without functional need.

---

## Security Review Checklist

Run this mentally before submitting any change:

```txt
[ ] Can a user escape filorafs.storage-path using filename input?
[ ] Can a user upload a dangerous file type?
[ ] Can a user overwrite an existing file?
[ ] Can a user delete unintended files?
[ ] Does any response expose internal paths?
[ ] Are real secrets present in code/docs/Postman?
[ ] Is API-key protection still active?
[ ] Are file streams closed properly?
[ ] Are errors controlled and non-leaky?
```

---

## Final Agent Output Requirements

When an AI agent completes work, its summary must include:

```txt
Changed files:
- ...

What changed:
- ...

Validation:
- Tests run, or reason tests were not run

Security notes:
- File path, upload, API key, or config implications
```

Do not claim tests passed unless they were actually run.

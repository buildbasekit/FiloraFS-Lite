# ARCHITECTURE.md

This document explains the architecture of **FiloraFS-Lite** for human maintainers and AI coding agents.

FiloraFS-Lite is a minimal Spring Boot backend for local filesystem-based file upload, streaming, deletion, listing, and metadata retrieval. It is designed as a starter/boilerplate, not as a full document-management platform.

---

## Technology Stack

```txt
Language:      Java 21
Framework:     Spring Boot
Build Tool:    Maven
API Style:     REST
Storage:       Local filesystem
Auth Model:    API key header
Database:      None
Default Port:  8000
```

The Maven configuration uses Spring Boot with the web starter and test starter. The project intentionally avoids persistence frameworks and heavy infrastructure.

---

## High-Level Runtime Flow

```txt
Client
  |
  | HTTP request with X-API-KEY
  v
ApiKeyFilter
  |
  | validates configured file.api.key
  v
FileController
  |
  | delegates business logic
  v
FileService
  |
  | reads/writes local filesystem under upload.path
  v
Configured upload directory
```

Every normal request is expected to include:

```txt
X-API-KEY: <configured-api-key>
```

The filter currently allows `OPTIONS` requests to pass through to support browser preflight requests.

---

## Package Structure

```txt
com.file
├── FileUploadApiApplication
├── controller
│   └── FileController
├── dtos
│   └── FileStream
├── filter
│   └── ApiKeyFilter
└── services
    └── FileService
```

### `FileUploadApiApplication`

Spring Boot entry point.

Responsibilities:

- Bootstraps the application.
- Enables component scanning under `com.file`.

It should stay small.

---

## API Layer

### `FileController`

Location:

```txt
src/main/java/com/file/controller/FileController.java
```

Base route:

```txt
/file
```

Current endpoints:

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/file` | Upload file using multipart field `file` |
| `GET` | `/file/{filename}` | Stream file content to response |
| `DELETE` | `/file/{filename}` | Delete file by stored filename |
| `GET` | `/file/list` | List stored filenames |
| `GET` | `/file/info/{filename}` | Return metadata for one file |

Controller responsibilities:

- Accept HTTP requests.
- Bind request path/body values.
- Delegate file operations to `FileService`.
- Set streaming response content type.
- Return API responses.

Controller should not contain deep filesystem logic.

Recommended future improvements:

- Use `ResponseEntity<?>` for explicit status codes.
- Avoid broad `catch (Exception)` blocks.
- Avoid `printStackTrace()` in request handling.
- Return controlled error responses for missing files and validation failures.

---

## Service Layer

### `FileService`

Location:

```txt
src/main/java/com/file/services/FileService.java
```

Responsibilities:

- Validate allowed file content types.
- Create the upload directory if missing.
- Generate UUID-based stored filenames.
- Save files to the configured storage path.
- Resolve file paths.
- Stream files from disk.
- Delete files from disk.
- List stored files.
- Return basic metadata.

Current allowed MIME types:

```txt
image/png
image/jpeg
application/pdf
image/webp
```

Current storage naming strategy:

```txt
<uuid><original-extension>
```

Example:

```txt
a5eaf6f1-11dc-4d44-a37c-75de31dcb6fd.png
```

Important security note: user-supplied filenames should not be trusted for storage names or path resolution. The current upload flow generates UUID filenames, which is good. Any direct filename lookup for stream/delete/info must still be protected against path traversal.

---

## DTO Layer

### `FileStream`

Location:

```txt
src/main/java/com/file/dtos/FileStream.java
```

Simple DTO carrying:

- `InputStream is`
- `String mimeType`

Used by `FileService#getFile` and consumed by `FileController#getFile`.

Recommended future improvement:

- Consider renaming `is` to `inputStream` for clarity.
- Ensure streams are closed safely with try-with-resources at the controller or service boundary.

---

## Filter Layer

### `ApiKeyFilter`

Location:

```txt
src/main/java/com/file/filter/ApiKeyFilter.java
```

Responsibilities:

- Read configured API key from `file.api.key`.
- Read request API key from `X-API-KEY`.
- Compare keys using `MessageDigest.isEqual`.
- Reject unauthorized requests with HTTP 401.
- Allow `OPTIONS` requests to pass through.

Current behavior:

```txt
Missing X-API-KEY       -> 401 Unauthorized
Wrong X-API-KEY         -> 401 Unauthorized
Matching X-API-KEY      -> request continues
OPTIONS request         -> request continues
```

Important note: because the filter is registered as a Spring `@Component`, it applies globally. If public endpoints are added later, the filter behavior must be adjusted deliberately.

---

## Configuration Architecture

Location:

```txt
src/main/resources/application.properties
```

Current properties:

```properties
spring.application.name=FiloraFS-Lite
server.port=8000
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
upload.path=<path-to-your-upload-folder>
file.api.key=<paste-here-strong-key>
```

### `upload.path`

Controls where files are stored.

Production expectations:

- Use an absolute path.
- Keep it outside public web roots.
- Ensure the app process has read/write permission.
- Avoid using source-code directories.
- Back up this directory if uploaded files are important.

### `file.api.key`

Controls API access.

Production expectations:

- Use a strong random value.
- Prefer environment variables or deployment secrets.
- Do not commit real values.
- Rotate leaked keys immediately.

---

## Request Flows

### Upload Flow

```txt
POST /file
multipart field: file
header: X-API-KEY
```

Flow:

```txt
Client sends multipart file
  -> ApiKeyFilter validates X-API-KEY
  -> FileController.saveFile receives MultipartFile
  -> FileService.saveFile validates MIME type
  -> FileService creates upload directory if missing
  -> FileService extracts extension from original filename
  -> FileService generates UUID filename
  -> FileService writes file to upload.path
  -> API returns stored filename
```

Security-sensitive points:

- MIME type can be spoofed.
- Original filename may contain unsafe characters.
- Extension should not be trusted as proof of content.
- Empty files may need explicit handling.
- Path traversal must be blocked.

---

### Stream/Download Flow

```txt
GET /file/{filename}
header: X-API-KEY
```

Flow:

```txt
Client requests stored filename
  -> ApiKeyFilter validates X-API-KEY
  -> FileController.getFile calls FileService.getFile
  -> FileService resolves file path
  -> If file is missing, current implementation tries default.png
  -> FileService probes MIME type
  -> FileController writes InputStream to HttpServletResponse
```

Security-sensitive points:

- Filename must not escape `upload.path`.
- Missing-file behavior must be documented.
- Stream must be closed safely.
- Response should not expose stack traces.

---

### Delete Flow

```txt
DELETE /file/{filename}
header: X-API-KEY
```

Flow:

```txt
Client requests deletion
  -> ApiKeyFilter validates X-API-KEY
  -> FileController.deleteFile delegates to FileService
  -> FileService resolves path
  -> If file exists, delete it
  -> API returns true/false
```

Security-sensitive points:

- Path traversal must be blocked.
- Deleting `default.png` or protected files may need policy checks if added.
- A boolean response is simple but not very expressive.

---

### List Flow

```txt
GET /file/list
header: X-API-KEY
```

Flow:

```txt
Client requests list
  -> ApiKeyFilter validates X-API-KEY
  -> FileService reads upload.path
  -> Files are filtered to normal files
  -> API returns names
```

Potential future improvements:

- Pagination for large directories.
- Sorting by name or modified time.
- Metadata DTOs instead of raw strings.

---

### Metadata Flow

```txt
GET /file/info/{filename}
header: X-API-KEY
```

Flow:

```txt
Client requests file info
  -> ApiKeyFilter validates X-API-KEY
  -> FileService resolves path
  -> FileService reads size, MIME type, last modified time
  -> API returns metadata map
```

Recommended future improvement:

- Use a typed response DTO instead of raw `Map`.
- Return 404 if file is missing.

---

## Current Design Strengths

- Small and easy to understand.
- No database dependency.
- UUID-based upload filenames reduce collision risk.
- API-key filter is simple and explicit.
- Multipart size limits are configured.
- Postman collection documents core API flow.
- Good starter for small projects and learning.

---

## Current Design Risks

AI agents must be aware of these risks before editing:

1. Path handling is string-based.
   - Any direct use of `{filename}` can be risky without normalization and base-path checks.

2. MIME validation uses `MultipartFile#getContentType()`.
   - This can be client-controlled.

3. `getFile` catches exceptions and can return `null`.
   - This may cause unclear controller behavior.

4. Controller catches broad exceptions and prints stack traces.
   - This is not production-grade error handling.

5. Raw `List` and raw `Map` are used.
   - Prefer typed generics in future changes.

6. API key examples in shared files must remain placeholders.
   - Do not commit real keys.

7. No pagination exists for `/file/list`.
   - Large upload folders may degrade performance.

8. No metadata persistence exists.
   - Metadata is derived from filesystem state at request time.

---

## Extension Boundaries

Allowed lightweight extensions:

- Better error handling.
- Safer path resolution.
- Stronger upload validation.
- Typed response DTOs.
- Better tests with temporary directories.
- Environment-variable-friendly configuration.
- Optional CORS configuration.
- Pagination for file listing.
- Improved README setup guide.

Do not add these unless explicitly requested:

- Database schema.
- User authentication.
- JWT or OAuth.
- S3/cloud storage.
- Redis/cache layer.
- Message queues.
- Admin UI.
- Multi-tenant storage.
- Virus scanning integrations.

Those belong in a heavier/pro edition or a separate project decision.

---

## Recommended Future Architecture Improvements

These are safe roadmap ideas, not mandatory changes:

1. Introduce typed DTOs:

```txt
UploadResponse
FileInfoResponse
ErrorResponse
```

2. Introduce a safe path resolver:

```txt
StoragePathResolver
```

Responsibilities:

- Normalize paths.
- Resolve filenames against configured root.
- Reject traversal attempts.

3. Introduce custom exceptions:

```txt
FileNotAllowedException
StoredFileNotFoundException
InvalidFilenameException
StorageException
```

4. Add a global exception handler:

```txt
@RestControllerAdvice
GlobalExceptionHandler
```

5. Replace raw maps/lists with typed generics.

6. Add filesystem tests using JUnit temporary directories.

---

## Architectural Principle

FiloraFS-Lite should remain:

```txt
Small enough to understand in minutes.
Secure enough to adapt responsibly.
Simple enough to reuse in real projects.
```

# Architecture

> Spring owns framework infrastructure. FiloraFS-Lite owns only file-storage-specific behavior.

This is a single-process local file API, with no metadata database, user model, or storage-provider abstraction. The [website](https://buildbasekit.com/docs/filorafs-lite/overview/) owns end-user documentation; this file owns repository design boundaries.

## Responsibilities

| Component | Responsibility |
| --- | --- |
| `FileUploadApiApplication` | Boot startup and configuration-properties scanning |
| `FiloraFSProperties` | Bind storage location and API key; reject blank required values |
| `ApiKeyFilter` | Shared-key boundary for file requests using Spring-normalized paths |
| `FileController` | HTTP mapping, delegation, attachment headers, and resource responses |
| `FileService` | Upload validation, generated names, safe file access, listing, and metadata |
| `FileMetadata` | Stable metadata response record, without internal paths |
| `ApiTestController` | Redirect the tester entry points to Spring-served static assets |

Spring Boot owns the embedded server, multipart parsing and limits, configuration loading, error handling, Actuator health, and graceful shutdown. Spring MVC owns routing, resource streaming, and static assets. There is no custom framework layer.

## Request and storage flows

File requests pass through `ApiKeyFilter` → `FileController` → `FileService` → configured storage directory. The filter compares `X-API-KEY` in constant time. Preflight `OPTIONS` requests pass through; no cross-origin policy is enabled by that exception. Tester resources and the minimal health endpoint are public. The detailed access boundary is in [SECURITY.md](SECURITY.md).

Uploads arrive as `MultipartFile`. The service checks extension, declared type, and a short signature, generates a UUID filename, reserves it without overwriting an existing entry, then calls `transferTo(Path)`. Failed transfers attempt to remove partial files. Spring's multipart infrastructure owns size enforcement.

Downloads resolve a single stored filename under the normalized root and reject links and non-regular files. The controller returns `ResponseEntity<Resource>` with Spring `MediaTypeFactory` resolution and an octet-stream fallback. Spring streams the content. Metadata uses the same MIME mapping; this mapping is descriptive, not content validation.

Listing, metadata, and deletion share the local storage boundary. Listing returns sorted regular filenames; metadata derives from the filesystem. Missing downloads or metadata return 404; deletion preserves its boolean response. Security controls and filesystem trust assumptions are maintained only in SECURITY.md.

## Configuration and extension boundaries

[application.properties](src/main/resources/application.properties) owns runnable defaults and delegates optional overrides to Spring Boot externalized configuration. No `.env` file is required. The configuration record keeps two simple checks; adding a validation framework for these alone would increase dependencies without meaningful benefit.

Tests cover application startup, configuration binding, HTTP/access behavior, and storage behavior using temporary directories. Extend the existing components and tests when requirements change. Do not introduce cloud providers, persistence, additional authentication frameworks, or generic interfaces for hypothetical future needs.

# Security model

This document owns FiloraFS-Lite's security controls, limitations, and deployment responsibilities. Lite is intended for simple, single-host file workflows; it is not a multi-user or distributed storage service.

## File handling

- Uploads use UUID filenames with an allowed extension. Original names never become storage paths. The destination is created exclusively before transfer, so an existing name is not overwritten; failed transfers attempt to remove partial output.
- Direct file operations accept one flat filename, reject absolute/nested paths, normalize under the configured root, and enforce containment. Downloads, metadata, and listing accept regular files without following symbolic links; deletion rejects links and directories.
- PNG, JPEG, PDF, and WebP uploads require matching extensions, declared media types, and lightweight signatures. Empty files and unsupported types are rejected. These checks do **not** provide malware scanning, full format validation, or protection against all polyglot files.
- Spring enforces configurable multipart file and request limits. The request limit includes multipart overhead. MIME headers and metadata use filename-based Spring resolution with an octet-stream fallback; MIME resolution is not a security validator.
- Downloads use attachment disposition. Metadata omits absolute paths. Default API error responses do not include exception details or stack traces.

## API-key boundary

- All `/file` operations require `X-API-KEY`; missing or incorrect keys return 401. The small filter uses Spring path normalization and constant-time byte comparison. `OPTIONS` passes through without enabling a permissive CORS policy.
- One shared key grants access to **all** stored files, including deletion. There are no users, ownership checks, per-file permissions, or application-level rate limits.
- `/api-test` and its assets are public; their API calls remain protected. The tester holds an entered key in page memory only and never receives the configured server secret automatically.
- `/actuator/health` is intentionally public and exposes status without components or details. Other Actuator endpoints and discovery are not exposed by default. Health is not a full storage-integrity or backup check.

## Deployment responsibilities

- Replace the known development key with a strong random `FILORAFS_API_KEY` in every deployed environment. Use a deployment secret manager or environment variables, HTTPS, and appropriate network access/rate limits. Rotate leaked keys.
- No `.env` file is required. If one exists locally, keep it ignored and private. Never commit secrets or log keys and request authorization headers. [.env.example](.env.example) contains only optional override examples.
- Keep storage outside public static-resource and source directories. Give the service account only the filesystem permissions it needs. Only trusted administrators and the application may modify the storage root **and its ancestors**; trusted mounted paths are configuration, not client input.
- Path and no-follow checks do not eliminate races against another local writer replacing files, links, or directories between validation and use. They do not defend against hostile hard links or a compromised host. Restrict local write access rather than treating the API as a filesystem sandbox.
- Provide persistent storage, capacity monitoring, backups, and retention/cleanup suited to the application. Align the platform's termination grace period with the configured shutdown timeout and verify storage permissions before deployment.

Detailed product configuration belongs in the [BuildBaseKit configuration guide](https://buildbasekit.com/docs/filorafs-lite/configuration/).

## Reporting a vulnerability

Do not disclose vulnerability details in a public issue. Use the repository's private vulnerability-reporting or GitHub Security Advisory channel when available. Otherwise contact maintainers privately through their verified project profile before sharing reproduction details. Include the affected version, impact, and steps to reproduce without real credentials or private files.

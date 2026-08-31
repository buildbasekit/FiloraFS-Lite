# FiloraFS-Lite

A minimal Spring Boot foundation for local file uploads, downloads, and management with API-key protection.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)](pom.xml)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](pom.xml)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[Website](https://buildbasekit.com/boilerplates/filora-fs-lite/) · [Documentation](https://buildbasekit.com/docs/filorafs-lite/overview/) · [Changelog](https://buildbasekit.com/boilerplates/filora-fs-lite/changelog/)

## Included

- Local filesystem storage with generated filenames
- Upload, download, list, metadata, and delete APIs
- File-type validation and path containment checks
- Shared API-key protection
- Browser API tester, Postman collection, and automated tests
- Spring Boot health endpoint and graceful shutdown

## Quick start

Requires Java 25 LTS and Git. Maven is included through the wrapper.

```bash
git clone https://github.com/buildbasekit/FiloraFS-Lite.git
cd FiloraFS-Lite
./mvnw spring-boot:run
```

Open [localhost:8080/api-test](http://localhost:8080/api-test). Runs with local storage and a development-only API key; no `.env` file or external service is required. On Windows, use `.\mvnw.cmd`.

Replace the development key before deployment; see [security boundaries](SECURITY.md).

→ [Quickstart guide](https://buildbasekit.com/docs/filorafs-lite/quickstart/)

## Documentation

Detailed product documentation lives on BuildBaseKit:

- [Overview](https://buildbasekit.com/docs/filorafs-lite/overview/)
- [Configuration](https://buildbasekit.com/docs/filorafs-lite/configuration/)
- [API reference](https://buildbasekit.com/docs/filorafs-lite/api/)
- [Architecture](https://buildbasekit.com/docs/filorafs-lite/architecture/)

## Project context

- [AGENTS.md](AGENTS.md)
- [AI_RULES.md](AI_RULES.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [SECURITY.md](SECURITY.md)

## BuildBaseKit

Part of [BuildBaseKit](https://buildbasekit.com/): focused Spring Boot foundations. Lite suits learning, prototypes, internal tools, and simple self-hosted applications.

## License

[MIT](LICENSE).

# Agent guide

FiloraFS-Lite is a small Spring Boot API for local filesystem storage. Preserve its usefulness as a readable backend foundation, not a storage platform.

> Spring owns framework infrastructure. FiloraFS-Lite owns only file-storage-specific behavior.

## Baseline and file map

- Java 25 LTS, Spring Boot 4.1.1, Maven wrapper; versions are defined in [pom.xml](pom.xml).
- `src/main/java/com/file/`: application entry point, configuration record, controllers, API-key filter, file service, and metadata record.
- `src/main/resources/application.properties`: Spring configuration and runnable local defaults; port 8080.
- `src/main/resources/static/api-test/`: plain HTML, CSS, and JavaScript client.
- `src/test/java/com/file/`: application startup, configuration, controller/filter, and filesystem service tests.
- [Postman collection](FiloraFS-Lite.postman_collection.json): executable file workflow.

Requests flow through `ApiKeyFilter` and `FileController` to `FileService`. File APIs live under `/file`; storage is one configured local directory. No database or cloud service is required.

## Read before editing

Read [README.md](README.md), [AI_RULES.md](AI_RULES.md), and the exact implementation and tests affected by the task. Use these focused references as needed; do not reload unchanged documents repeatedly:

| Source | Owns |
| --- | --- |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Component responsibilities and extension boundaries |
| [SECURITY.md](SECURITY.md) | File-security controls, limitations, deployment responsibilities, disclosure |
| [AGENT_CONTRIBUTING.md](AGENT_CONTRIBUTING.md) | Agent workflow and verification |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Human contribution guidance |
| [BuildBaseKit documentation](https://buildbasekit.com/docs/filorafs-lite/overview/) | Detailed end-user setup, configuration, and API reference |

Read SECURITY.md before changing file I/O or access control. Verify behavior from implementation rather than assuming the documentation is current.

## Commands

```bash
./mvnw test
./mvnw clean verify
./mvnw spring-boot:run
```

Use `.\mvnw.cmd` on Windows. No `.env` file is required. Tests must use temporary storage and synthetic credentials, independently of local configuration.

## Definition of done

Changes remain focused, preserve API compatibility and the security boundary, and pass relevant tests plus final wrapper verification. Check `git diff --check`, exclude secrets and generated files, and update only the documentation that owns the changed behavior. Report validation results and any remaining risks honestly.

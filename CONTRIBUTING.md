# Contributing

Keep contributions focused and easy to review. FiloraFS-Lite prioritizes simple, readable Spring Boot code and a small local-storage scope.

- Follow the existing [architecture](ARCHITECTURE.md); let Spring handle framework infrastructure.
- Preserve API compatibility and [security boundaries](SECURITY.md).
- Avoid unnecessary dependencies, abstractions, and unrelated refactors. Discuss broader features before implementing them.
- Add or update tests when behavior changes; use temporary storage and synthetic credentials.
- Verify API changes through the included [Postman collection](FiloraFS-Lite.postman_collection.json) or browser tester.
- Keep detailed product documentation on [BuildBaseKit](https://buildbasekit.com/docs/filorafs-lite/overview/), and update the relevant source when behavior changes.

Before submitting:

```bash
./mvnw clean verify
```

Use `.\mvnw.cmd` on Windows. See [README.md](README.md) for the quick start.

Explain the problem, what changed, and how you verified it in your pull request. Never include real secrets or runtime uploads. Report vulnerabilities privately using [SECURITY.md](SECURITY.md). AI-assisted contributions follow [AGENTS.md](AGENTS.md) and remain the contributor's responsibility to review.

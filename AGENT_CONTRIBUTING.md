# Agent contribution workflow

Use [AGENTS.md](AGENTS.md) for context, [AI_RULES.md](AI_RULES.md) for constraints, and [ARCHITECTURE.md](ARCHITECTURE.md) for design. Human contributors use [CONTRIBUTING.md](CONTRIBUTING.md).

1. Inspect the working tree before modifying it. Preserve unrelated and pre-existing changes. Identify affected files, read their implementation and tests, and assess API, configuration, and security impact.
2. Make the smallest change that satisfies the request within the existing architecture. Avoid unrelated refactors. Add or update tests only for meaningful changed behavior.
3. Run relevant tests with `./mvnw test`; use `./mvnw clean verify` for final verification. Use `.\mvnw.cmd` on Windows. Do not rely on a globally installed Maven or local secrets.
4. When API behavior changes, check the complete upload → list → metadata → download → delete workflow through the included tester or Postman collection, plus relevant failure cases. Review [SECURITY.md](SECURITY.md) when file handling or access changes.
5. Update the canonical source for changed documentation. Product setup/configuration/API detail belongs on the website; repository files own contributor guidance and implementation boundaries. Report website drift if it cannot be updated within the task.
6. Check `git status` and `git diff --check`. Exclude generated artifacts and credentials, preserve wrapper executable permissions, and report changes, behavior/security implications, tests actually run, and blockers. Do not commit or push unless asked.

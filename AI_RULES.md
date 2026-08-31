# AI coding rules

These are the non-negotiable rules for work on FiloraFS-Lite. Operational context is in [AGENTS.md](AGENTS.md).

1. **Spring first.** Use Spring Boot and Spring Framework infrastructure before writing custom equivalents: multipart handling, resource streaming, HTTP responses, configuration binding, and lifecycle management.
2. **Keep Lite small.** No speculative features, generic storage interfaces, extra layers, DTO hierarchies, or dependencies without a concrete need. Do not add databases, cloud storage, JWT, Spring Security, queues, Docker, or CI/CD without explicit scope.
3. **Preserve contracts.** Keep file routes, multipart field `file`, response shapes, status semantics, and `X-API-KEY` behavior compatible unless a requested or necessary correction is documented and tested.
4. **Protect secrets.** Never print, log, return, or commit real credentials. Leave local `.env` files untouched and ignored; examples use placeholders or clearly synthetic development values.
5. **Preserve file security.** Keep generated storage names, collision protection, flat-name containment, and symlink/non-regular-file checks. Never use original filenames as storage paths or expose absolute paths. See [SECURITY.md](SECURITY.md).
6. **Preserve upload validation.** Reject empty, unsupported, mismatched, and signature-spoofed uploads. Keep Spring multipart limits. Do not expand allowed types without a requirement.
7. **Keep access explicit.** File operations require the API key, including alternate URL encodings. Public tester or health routes must not grant access to file contents. Keep the small constant-time key check.
8. **Write readable code.** Use constructor injection, final dependencies, typed collections, and JDK `Path`/`Files` for storage. Do not swallow errors, return accidental nulls, or expose stack traces.
9. **Verify behavior.** Use the Maven wrapper and existing Spring test infrastructure. Cover meaningful behavior and security changes with isolated tests; avoid implementation-detail tests.
10. **Respect documentation ownership.** Link to canonical product documentation rather than copying it. Keep repository references synchronized. CONTRIBUTING.md remains the human guide; agent workflow belongs in [AGENT_CONTRIBUTING.md](AGENT_CONTRIBUTING.md).

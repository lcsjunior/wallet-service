# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=WalletControllerIntegrationTest

# Build and run with Docker
docker compose up --build
```

## Architecture

Spring Boot 3.5.16 REST service, Java 21.

Database: H2 relational (embedded). Schema must support full audit traceability.

Idempotency: mutation endpoints (deposit, withdraw, transfer) use a `correlationId` header so retries are safe.

Concurrency: wallet balance updates are guarded by optimistic locking (JPA `@Version` on `Wallet`), not database row locks. Concurrent modifications to the same wallet fail at commit and surface as `409 Conflict` (`ObjectOptimisticLockingFailureException` handled in `GlobalExceptionHandler`); clients retry safely thanks to `correlationId` idempotency.

> Code conventions (package naming, layering, DTOs, testing, Docker build) are defined in `.claude/rules/code-conventions.md`.

## Temporary Rules

- **Integration tests only (temporary)**: Unit test classes (suffixed `Test`) MUST NOT be created while this rule is active. Only integration test classes (suffixed `IntegrationTest`) are to be authored or maintained. This rule is temporary and supersedes — without editing — the permanent unit-testing requirements in `.claude/rules/code-conventions.md` until it is explicitly lifted.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan:
`specs/003-idempotency-cache-simplification/plan.md`
<!-- SPECKIT END -->

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build (runs spotless:check in verify — unformatted code fails the build)
./mvnw clean package

# Auto-format before committing
./mvnw spotless:apply

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=WalletControllerIntegrationTest

# Build and run with Docker (app + Redis)
./mvnw clean package && docker compose up --build
```

The app needs Redis at `REDIS_HOST:REDIS_PORT` (default `localhost:6379`) for the
idempotency cache. `docker compose` provides it; running locally requires it up
separately. Actuator's Redis health check is disabled, so a missing Redis does not
mark the app down.

## Stack

Spring Boot 3.5.16, Java 21, Maven wrapper.

| Concern | Choice |
| --- | --- |
| Persistence | H2, file-based at `./data/wallet-service`, `ddl-auto=update` |
| Cache | Redis via Spring Cache, TTL 24h |
| Mapping | MapStruct 1.6.3 (annotation processor) |
| API docs | springdoc-openapi (`/swagger-ui.html`) |
| Formatting | Spotless + google-java-format, enforced at `verify` |
| Coverage | JaCoCo, report bound to `test` |

## API

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/v1/wallets` | Create wallet |
| GET | `/v1/wallets/{walletId}/balance` | Current balance |
| POST | `/v1/wallets/{walletId}/deposits` | Requires `Correlation-Id` |
| POST | `/v1/wallets/{walletId}/withdrawals` | Requires `Correlation-Id` |
| POST | `/v1/transfers` | Requires `Correlation-Id` |

## Architecture

**Audit traceability.** Every balance change writes a `WalletTransaction` row. The
schema must keep that history intact — never mutate or delete transaction rows to
"fix" a balance.

**Idempotency.** The three mutation endpoints take a `Correlation-Id` HTTP header.
`TransactionService` hashes the request parameters into a fingerprint and stores it
with the correlation id in `IdempotencyEntry`:

- same id, same fingerprint → the retry is a no-op, returns success
- same id, different fingerprint → `409 Conflict` (`CORRELATION_ID_CONFLICT`)

`IdempotencyRepository` is the only cached repository.

**Concurrency.** Wallet balance updates use optimistic locking (JPA `@Version` on
`Wallet`), not database row locks. Concurrent modifications to the same wallet fail
at commit and surface as `409 Conflict`
(`ObjectOptimisticLockingFailureException`, handled in `GlobalExceptionHandler`);
clients retry safely thanks to `Correlation-Id` idempotency.

**Errors.** All business failures go through the `ErrorCode` enum, which pairs an
HTTP status with a message key resolved from `messages.properties`:

| Code | Status |
| --- | --- |
| `WALLET_NOT_FOUND` | 404 |
| `INSUFFICIENT_BALANCE` | 422 |
| `CORRELATION_ID_CONFLICT` | 409 |
| `SAME_WALLET_TRANSFER` | 400 |
| `MISSING_REQUIRED_HEADER` | 400 |
| `VALIDATION_ERROR` | 400 |

> Code conventions (package naming, layering, DTOs, testing, caching, error
> responses, Docker build) are defined in `.claude/rules/code-conventions.md`.
> Read it before writing code.

## Temporary Rules

- **Integration tests only (temporary)**: Unit test classes (suffixed `Test`) MUST
  NOT be created while this rule is active. Only integration test classes (suffixed
  `IntegrationTest`) are to be authored or maintained. This rule is temporary and
  supersedes — without editing — the permanent unit-testing requirements in
  `.claude/rules/code-conventions.md` until it is explicitly lifted.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Conventions

Project rules live in `.claude/rules/code-conventions.md` — read it before writing code.
This file describes how the codebase works; it states no rules of its own.

## Commands

```bash
./mvnw clean package                  # build the JAR (required before docker compose up)
./mvnw test                           # run the suite (Docker must be running)
./mvnw verify                         # test + spotless:check
./mvnw spotless:apply                 # format (google-java-format)
./mvnw clean verify sonar:sonar       # what CI runs

./mvnw test -Dtest=TransferControllerIntegrationTest
./mvnw test -Dtest=TransferControllerIntegrationTest#shouldRejectWhenBalanceIsInsufficient

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # local run, needs Redis on :6379
docker compose up --build                               # app + Redis together
```

Tests start a `redis:7-alpine` Testcontainer, so Docker must be up. Formatting is also
applied by a pre-commit hook — enable it once per clone with
`git config core.hooksPath .githooks`.

## Architecture

Layering is `controller → service → repository`, with MapStruct between entity and DTO.
Four controllers under `/v1`, two services, three repositories.

### Two services, split by what they own

- `WalletService` — wallet lifecycle and balance reads. The only user of `WalletMapper`.
- `TransactionService` — all money movement (`deposit`, `withdraw`, `transfer`). Each
  method is a single `@Transactional` unit that touches wallets, the audit ledger and the
  idempotency table together.

`TransferController` sits at `/v1/transfers` rather than under a wallet, because a transfer
belongs to two wallets; deposits and withdrawals are nested under `/v1/wallets/{walletId}`.

### Idempotency

Every mutation requires a `Correlation-Id` header (constant in `Constants`, so the literal
appears once). Each operation starts by building an `IdempotencyEntry` through
`IdempotencyEntry.of`, whose factory derives a *fingerprint* string from the operation
type, the wallet key (`from->to` for transfers) and the plain-string amount.
`TransactionService` then:

1. looks the correlation id up in `IdempotencyRepository`;
2. absent → proceeds and, at the end, persists that same entry;
3. present with the same fingerprint → returns silently, no balance change;
4. present with a different fingerprint → `CORRELATION_ID_CONFLICT` (409).

The fingerprint is what makes "same key, different body" a conflict instead of a silent
no-op — it carries every business parameter of the operation.

### Redis is an accelerator, never the truth

The database is the source of truth. `IdempotencyRepository` is the *only* cached
repository — `@Cacheable` on `findById`, `@CachePut` on `save`. `CacheConfig` installs a
`LoggingCacheErrorHandler`, so a Redis outage degrades to database lookups instead of
failing the request. `CACHE_TYPE=none` disables caching entirely and the service still
behaves correctly. Redis health is deliberately excluded from the actuator health group.

### Concurrency

`Wallet` carries a JPA `@Version`. Concurrent updates to the same wallet lose the
optimistic lock and surface as `ObjectOptimisticLockingFailureException`, which
`GlobalExceptionHandler` turns into `409 Conflict`. Retrying with the same
`Correlation-Id` is safe by design — that pairing is the concurrency story, and there is
no pessimistic locking anywhere.

### Auditability

Balances are mutated in place on `Wallet`, but every movement also appends an
`@Immutable` `WalletTransaction` carrying `type`, `amount`, `balanceAfter`,
`correlationId` and, for transfers, `peerWalletId`. A transfer writes **two** rows —
`TRANSFER_DEBIT` and `TRANSFER_CREDIT` — so each wallet's history reads standalone. The
ledger is the reconciliation trail.

Note the two enums: `TransactionType` (ledger rows, four values including the two transfer
legs) versus `OperationType` (the first segment of the fingerprint, three values, never
persisted on its own). They are not interchangeable.

### Errors

`ServiceException.of(message, httpStatus)` pairs the `detail` text with the status at the
point that detects the failure, and `GlobalExceptionHandler` emits them as an RFC 7807
`ProblemDetail`, so the handlers do not grow per error. The texts themselves live in
`constants/Messages`; the status does not, so the same message can be thrown with
different statuses. There is no `MessageSource` — the service speaks English only, and
bean-validation messages sit inline on the request records.

`GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` and overrides
`handleMethodArgumentNotValid` to attach the `errors` array via `FieldErrorMapper`.

### Money on the wire

Amounts are `BigDecimal` serialized as JSON **strings** (`@JsonFormat(shape = STRING)` on
responses) to avoid float rounding in clients. Requests validate with `@Positive` plus
`@Digits(integer = 17, fraction = 2)`, and direction comes from the endpoint rather than
the payload. `Constants.ZERO_AMOUNT` uses `RoundingMode.UNNECESSARY`, which makes any
accidental rescale fail loudly.

## Persistence and profiles

H2 in-memory in PostgreSQL compatibility mode, `ddl-auto=update` — schema comes from the
entities, there are no migration scripts, and every restart starts empty. `open-in-view`
is off, so lazy associations only resolve inside the service transaction.

- default — ECS JSON logging, H2 console off
- `dev` — plain-text logs, H2 console at `/h2-console`, SQL logging; used by `docker compose`
- `test` — `ddl-auto=create-drop`, plain-text logs

## Tests

`AppTests` is the shared base: `@SpringBootTest` + `MockMvc`, `test` profile, a static
Redis container wired by `@ServiceConnection`, a `flushAll` before each test, and the
`balanceOf(walletId)` helper for asserting balances. Integration classes extend it and
declare `@Sql({"/mock/sql/clear-tables.sql", "/mock/sql/<name>-seed.sql"})`, so each test
starts from a known database and an empty cache — that reset is what lets tests reuse
fixed UUIDs and correlation ids.

`JsonUtils` reads payloads from the filesystem path `src/test/resources/mock/`, not the
classpath, so the suite only passes when run from the project root.

## CI

`.github/workflows/ci.yml` runs `clean verify sonar:sonar` on pull requests to `main` and
pushes to it, waiting on the SonarCloud quality gate. `verify` includes `spotless:check`,
so unformatted code fails the build.

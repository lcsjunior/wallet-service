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

Tests start a `redis:7-alpine` Testcontainer, so Docker must be up. A pre-commit hook also
formats staged files — enable it once per clone with
`git config core.hooksPath .githooks`.

## Architecture

Layering is `controller → service → repository`, with MapStruct between entity and DTO.
Four controllers under `/v1`, three services, three repositories.

- `WalletService` — wallet creation. The only user of `WalletMapper`.
- `TransactionService` — all money movement (`deposit`, `withdraw`, `transfer`). Each
  method is one `@Transactional` unit touching wallets, the ledger and the idempotency
  table together.
- `IdempotencyService` — replay detection and entry persistence. The only user of
  `IdempotencyRepository`; it declares no transaction of its own and runs inside the
  caller's.

`TransferController` sits at `/v1/transfers` rather than under a wallet, because a
transfer belongs to two wallets; deposits and withdrawals nest under
`/v1/wallets/{walletId}`.

`WalletRepository.findWallet` is a `default` method that throws `WALLET_NOT_FOUND`/404
instead of returning `Optional` — callers never handle absence themselves.

### Idempotency

Every endpoint requires a `Correlation-Id` header, but only the three money movements key
idempotency off it — wallet creation merely carries it into the logs. `IdempotencyEntry.of` derives a
*fingerprint* — `operationType:key:amount`, where `key` is the wallet id, or
`from->to` for transfers. `IdempotencyService.isReplay` then:

1. looks the correlation id up in `IdempotencyRepository`;
2. absent → proceeds, and persists the entry at the end of the method;
3. present, same fingerprint → returns `REPLAYED` without touching a balance;
4. present, different fingerprint → `CORRELATION_ID_CONFLICT` (409).

The fingerprint carries every business parameter, which is what makes "same key,
different body" a conflict instead of a silent no-op.

Each method returns `TransactionOutcome` (`APPLIED` / `REPLAYED`) and the controllers echo
`outcome.isReplayed()` as `Idempotent-Replayed: true|false` on the `204`. The response has
no body, so that header is the only replay signal a client gets.

Note the two enums: `TransactionType` (ledger rows, four values including both transfer
legs) and `OperationType` (first segment of the fingerprint, three values). They are not
interchangeable.

### Redis

The database is the source of truth. `IdempotencyRepository` is the only cached repository
— `@Cacheable` on `findById` with `unless = "#result == null"`, `@CachePut` on `save`.
`CacheConfig` installs a `LoggingCacheErrorHandler`, so a Redis outage degrades to database
lookups instead of failing the request. `CACHE_TYPE=none` disables caching entirely and the
service still behaves correctly. Redis health is excluded from the actuator health group.

### Concurrency

`Wallet` carries a JPA `@Version`. Concurrent updates to the same wallet lose the
optimistic lock and surface as `ObjectOptimisticLockingFailureException`, which
`GlobalExceptionHandler` turns into `409 Conflict`. There is no pessimistic locking
anywhere; retrying with the same `Correlation-Id` is what makes that safe.

### Auditability

Balances are mutated in place on `Wallet` via `credit`/`debit`, and every movement also
appends an `@Immutable` `WalletTransaction` carrying `type`, `amount`, `balanceAfter`,
`correlationId` and, for transfers, `peerWalletId`. A transfer writes **two** rows —
`TRANSFER_DEBIT` and `TRANSFER_CREDIT` — so each wallet's history reads standalone.

### Correlation id

`RequestLoggingFilter` runs at `HIGHEST_PRECEDENCE` and carries the `Correlation-Id` header into
the MDC under `correlationId`, clearing it in a `finally`. Nothing is generated when the header is
absent, since every endpoint requires it. ECS console logging renders MDC entries as top-level
fields, so every application log line of a request is filterable by that id. Request and response
payloads are deliberately never logged.

### Errors

`ServiceException.of(message, httpStatus)` pairs the `detail` text with the status at the
point that detects the failure, so `GlobalExceptionHandler` does not grow a handler per
error. The status is not carried alongside the text, so the same message can be thrown with
different statuses. Titles are `Business violation` for domain failures and
`Validation error` for invalid payloads or missing headers.

`GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` and overrides
`handleMethodArgumentNotValid` to attach the `errors` array via `FieldErrorMapper`. There
is no `MessageSource` — English only, and bean-validation messages sit inline on the
request records.

### Money on the wire

Request records validate amounts with `@Positive` plus `@Digits(integer = 17, fraction =
2)`, so extra decimals are rejected rather than rounded. `Constants.ZERO_MONEY` uses
`RoundingMode.UNNECESSARY`, so an accidental rescale fails loudly.

## Persistence and profiles

H2 in-memory in PostgreSQL compatibility mode, `ddl-auto=update` — schema comes from the
entities, there are no migration scripts, and every restart starts empty. `open-in-view` is
off, so lazy associations only resolve inside the service transaction.

- default — ECS JSON logging, H2 console off
- `dev` — SQL logging, H2 console at `/h2-console`; used by `docker compose`
- `test` — `ddl-auto=create-drop`, plain-text logs

## Tests

`AppTests` is the shared base: `@SpringBootTest` + `MockMvc`, `test` profile, a static
Redis container wired by `@ServiceConnection`, a `flushAll` before each test, and a
`balanceOf(walletId)` helper. Integration classes extend it, so each test starts from a
known database and an empty cache — that reset is what lets tests reuse fixed UUIDs and
correlation ids.

`JsonUtils.loadJson` reads from the filesystem path `src/test/resources/mock/`, not the
classpath, so the suite only passes when run from the project root.

## CI

`.github/workflows/ci.yml` runs `clean verify sonar:sonar` on pull requests to `main` and
pushes to it, waiting on the SonarCloud quality gate. `verify` includes `spotless:check`,
so unformatted code fails the build.

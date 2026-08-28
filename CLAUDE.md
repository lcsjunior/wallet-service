# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Conventions

Project rules live in `.claude/rules/code-conventions.md` — read it before writing code.
This file describes how the codebase works; it states no rules of its own.

## Commands

```bash
./mvnw clean package                  # build the JAR (required before docker compose up)
./mvnw test                           # run the suite
./mvnw verify                         # test + spotless:check
./mvnw spotless:apply                 # format (google-java-format)
./mvnw clean verify sonar:sonar       # what CI runs

./mvnw test -Dtest=TransferControllerIntegrationTest
./mvnw test -Dtest=TransferControllerIntegrationTest#shouldRejectWhenBalanceIsInsufficient

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # local run
docker compose up --build                               # app in a container
```

The suite needs no container of its own. A pre-commit hook formats staged files — enable it
once per clone with `git config core.hooksPath .githooks`.

## Architecture

Layering is `controller → service → repository`, with MapStruct between entity and DTO.
Four controllers under `/v1`, two services, two repositories.

- `WalletService` — wallet creation. The only user of `WalletMapper`.
- `TransactionService` — all money movement (`deposit`, `withdraw`, `transfer`). Each
  method is one `@Transactional` unit touching the wallet and the ledger together, and
  returns `void`.

`TransferController` sits at `/v1/transfers` rather than under a wallet, because a
transfer belongs to two wallets; deposits and withdrawals nest under
`/v1/wallets/{walletId}`.

`WalletRepository.findWallet` is a `default` method that throws `WALLET_NOT_FOUND`/404
instead of returning `Optional` — callers never handle absence themselves.

### Idempotency

Every endpoint requires an `Idempotency-Key` header — a UUID — and no mutation can be applied
twice under the same one. There is one mechanism for all four: a unique constraint, and no read
before the insert. The database is the only thing deciding, which is also what covers two
concurrent requests carrying the same key.

- creation — `Wallet.idempotencyKey` is `unique`; `WalletService.createWallet` calls
  `saveAndFlush` and a repeat loses the constraint;
- movements — `uk_wallet_transaction_wallet_idempotency_key` on `(wallet_id, idempotency_key)`;
  each ledger insert is a plain `save`, so a repeat loses the constraint when the transaction
  commits and the balance change rolls back with it.

Either way `GlobalExceptionHandler` turns the `DataIntegrityViolationException` into `409`
with `ENTITY_CONFLICT`. Nothing replays: there is no `Idempotent-Replayed` header and no
outcome type — a movement either applies and answers `204`, or conflicts and answers `409`.

Two consequences of letting the constraint decide. A client that retries after a timeout gets
`409` rather than the original result, and there is no endpoint to look either up. And the
domain rules run first, so a retried movement whose balance no longer covers it answers `422`
before the constraint is ever reached.

The ledger constraint is per wallet rather than global because a transfer writes two rows
under one key — see Auditability. So the same key on two *different* wallets is accepted; it is
one request per wallet that is guaranteed.

### Concurrency

`Wallet` carries a JPA `@Version`. Concurrent updates to the same wallet lose the
optimistic lock and surface as `ObjectOptimisticLockingFailureException`, which
`GlobalExceptionHandler` turns into `409 Conflict`. There is no pessimistic locking
anywhere; retrying with the same `Idempotency-Key` is what makes that safe.

### Auditability

Balances are mutated in place on `Wallet` via `credit`/`debit`, and every movement also
appends an `@Immutable` `WalletTransaction` carrying `type`, `amount`, `balanceAfter`,
`idempotencyKey` and, for transfers, `peerWalletId`. A transfer writes **two** rows —
`TRANSFER_DEBIT` and `TRANSFER_CREDIT` — so each wallet's history reads standalone. That is
why `idempotency_key` alone is not unique on the ledger; the constraint is
`(wallet_id, idempotency_key)`, which still holds one row per wallet per request.

### Correlation id

`Correlation-ID` is a separate header from `Idempotency-Key`: optional, free-form `String`, and
it never reaches a service — tracing only. `RequestLoggingFilter` runs at `HIGHEST_PRECEDENCE` and
carries **both** headers into the MDC, under `correlationId` and `idempotencyKey`, clearing them in
a `finally`. Neither is generated when absent, so those requests simply log without the field —
which for `idempotencyKey` only happens on a request the controller is about to reject with `400`.
ECS console logging renders MDC entries as top-level fields, so every application log line of a
request is filterable by either id. Request and response payloads are deliberately never logged.

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

`AppTests` is the shared base: `@SpringBootTest` + `MockMvc`, `test` profile, and a
`balanceOf(walletId)` helper. Integration classes extend it and each runs
`/mock/sql/clear-tables.sql` before every test, so each test starts from a known database —
that reset is what lets tests reuse fixed UUIDs and correlation ids.

`JsonUtils.loadJson` reads from the filesystem path `src/test/resources/mock/`, not the
classpath, so the suite only passes when run from the project root.

## CI

`.github/workflows/ci.yml` runs `clean verify sonar:sonar` on pull requests to `main` and
pushes to it, waiting on the SonarCloud quality gate. `verify` includes `spotless:check`,
so unformatted code fails the build.

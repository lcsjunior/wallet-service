# Wallet Service

RESTful microservice for wallet management — supports deposits, withdrawals, and transfers between users.

> Programming challenge. Java 21 + Spring Boot 3.5.

## Functional Requirements

- **Create Wallet** — open a new wallet for a user
- **Retrieve Balance** — get the current balance of a wallet
- **Retrieve Historical Balance** — get the balance at a specific point in the past
- **Deposit Funds** — add money to a wallet
- **Withdraw Funds** — remove money from a wallet
- **Transfer Funds** — move money between user wallets

## Non-Functional Requirements

- **Idempotency** — mutation operations (deposit, withdraw, transfer) are idempotent via a `correlationId` header; retrying the same request produces the same result
- **Mission-critical reliability** — downtime directly impairs platform operations
- **Full traceability** — all operations must be auditable for balance reconciliation

## Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 (Web, Data JPA, Actuator) |
| Database | H2 (relational, embedded) |
| Mapping | MapStruct |
| API Docs | SpringDoc OpenAPI (`/swagger-ui.html`) |
| Build | Maven Wrapper |

## Quick Start

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run
```

Access: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Docker

```bash
./mvnw clean package
docker compose up --build
```

## Tests

```bash
./mvnw test
```

> Code conventions (architecture layout, DTOs, testing conventions) are defined in `.claude/rules/code-conventions.md`.

## API Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/wallets` | Create a wallet for a user |
| `GET` | `/wallets/{userId}/balance` | Current balance (optionally `?asOf=<ISO-8601 instant>` for historical balance) |
| `POST` | `/wallets/{userId}/deposits` | Deposit funds (requires `Correlation-Id` header) |
| `POST` | `/wallets/{userId}/withdrawals` | Withdraw funds (requires `Correlation-Id` header) |
| `POST` | `/transfers` | Transfer funds between two users (requires `Correlation-Id` header) |

Full request/response contract: `specs/001-wallet-core-operations/contracts/wallet-api.md`.

## Design Decisions

- **Append-only transaction log (`WalletTransaction`)** — every deposit, withdrawal, and transfer leg writes an immutable row carrying the wallet's `balanceAfter`. `Wallet.balance` is a denormalized cache for O(1) current-balance reads, but the transaction log is the source of truth for auditing and for historical balance queries (look up the latest row with `createdAt <= asOf`). This directly satisfies the "full traceability" non-functional requirement.
- **Idempotency via `IdempotencyRecord`** — every mutating request carries a `Correlation-Id`; the first successful execution is cached (keyed by the id, with a fingerprint of the request parameters) and replayed verbatim on retry. A retry with the same id but different parameters is rejected with `409`. This satisfies the "safe to retry" requirement without a distributed lock or external cache.
- **Pessimistic row locking for concurrency** — deposits/withdrawals lock the affected wallet row (`SELECT ... FOR UPDATE`) before validating and mutating the balance; transfers lock both wallets in a deterministic order (by `userId`) to avoid deadlocks. This guarantees the balance can never go negative under concurrent requests to the same wallet (verified by `TransactionServiceConcurrencyTest`, which fires two concurrent withdrawals at the same wallet).
- **BigDecimal, scale 2, reject rather than round** — all monetary values are `BigDecimal` with exactly 2 decimal places. Bean Validation (`@Digits(fraction = 2)`) rejects any request with more precision (e.g., `0.015`) instead of silently rounding it — the service never guesses which fraction of a cent to discard.
- **H2 in file mode at runtime** — the embedded database persists to `./data/wallet-service` so the audit trail survives a process restart; the test profile uses H2 in-memory for isolation and speed.
- **Mission-critical availability** — out of scope for a single-node embedded-database demo service; the design (stateless service layer, no in-memory session state) does not preclude running multiple replicas against a shared database in a real deployment, but that infrastructure is not part of this challenge.

## Trade-offs Made Due to Time Constraints

- **Idempotency records are only persisted for successful operations.** A rejected attempt (e.g., insufficient balance) has no financial side effect to deduplicate, so it is re-validated fresh on every retry rather than being cached. This keeps the idempotency logic simple while still guaranteeing "no duplicate financial effect on retry" (the property that actually matters).
- **No authentication/authorization layer.** The service trusts the caller-supplied `userId`; user identity/session management is assumed to be handled by an upstream system, consistent with this being a wallet microservice rather than a full platform.
- **Single currency, no multi-tenancy.** Each user has at most one wallet in one implicit currency; multi-currency support is out of scope.
- **No pagination/listing endpoints** for transaction history — only point-in-time balance lookups, since that is what the assignment's functional requirements ask for.

## Time Spent

*(fill in your own rough estimate of hours invested before submitting, as requested by the assignment)*

## TODO

- [ ] Add pagination support for transaction history endpoints
- [ ] Format code according to `.claude/rules/code-conventions.md`
- [ ] Use cache/Redis for idempotency
- [ ] `userId` should be a UUID
- [ ] Rename factory methods to `of` (e.g., `Wallet.createNew` → `Wallet.of`, `IdempotencyRecord.create` → `IdempotencyRecord.of`)
- [ ] The current implementation is bad and should be redone as a clean implementation, similar to the `legacy` branch
- [ ] Every validation method must start with `validate`
- [ ] Add troubleshooting logs similar to the `legacy` branch, with a mandatory prefix
- [ ] Prefer `var` over explicit types for local variables
- [ ] Bean validation annotations must use customized messages from `messages.properties`, see `legacy` branch
- [ ] Every validation that throws an exception must be extracted into a separate `validate` method
- [ ] Exception methods must not expose/return `userId`
- [ ] Add a pre-commit/build hook to auto-format Java code (e.g., Spotless with Google Java Format)
- [ ] `GlobalExceptionHandler` should not hardcode error details — they should come from the exception itself; simplify `new ErrorResponse` creation with a builder or factory method
- [ ] Move all exception messages and error codes (e.g., `CORRELATION_ID_CONFLICT`) out of hardcoded strings into an enum
- [ ] Remove wallet lookups by `userId` — a `userId` may have N wallets, so wallets should always be looked up by wallet `id` instead
- [ ] Every repository must be an interface; concrete repository implementation classes must be suffixed `Impl`
- [ ] Remove `asOf` from the balance lookup — only the wallet's current balance is needed
- [ ] Review all `@Transactional` methods
- [ ] Move all integration test JSON payloads into dedicated `.json` files, per `.claude/rules/code-conventions.md`
- [ ] Remove all test classes suffixed `Test`, except controller integration tests, which must be suffixed `IntegrationTest`
- [ ] No method name may exceed 38 characters

# Wallet Service

[![CI](https://github.com/lcsjunior/wallet-service/actions/workflows/ci.yml/badge.svg)](https://github.com/lcsjunior/wallet-service/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=lcsjunior_wallet-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=lcsjunior_wallet-service)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=lcsjunior_wallet-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=lcsjunior_wallet-service)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)](https://spring.io/projects/spring-boot)

A RESTful microservice that manages wallets: deposits, withdrawals, and transfers between
users, built to be safe to retry and easy to reconcile.

> [!NOTE]
> This is a programming challenge, not a production payments system. It uses an
> in-memory database and a single-node design.

## Why this exists

Money movement APIs fail in specific, well-known ways: a client retries a timed-out
request and double-charges someone, two concurrent requests race on the same balance, or
an operator can't tell after the fact what actually happened to a wallet. Wallet Service
is built around three answers to those problems:

- Every mutation is **idempotent** by a client-supplied correlation id, so retries are
  free.
- Every balance update is **optimistically locked**, so concurrent writes fail loudly
  instead of silently corrupting a balance.
- Every movement is **appended to an immutable ledger**, so balances can always be
  reconciled after the fact.

## Features

- **Wallets** — create a wallet for a user; a user may hold more than one
- **Deposits & withdrawals** — single-wallet money movement
- **Transfers** — move funds between two wallets in one atomic operation
- **Idempotency** — retry any mutation safely with a `Correlation-Id` header; replays are
  flagged, never rejected
- **Optimistic concurrency** — concurrent updates to the same wallet fail with `409`
  instead of silently overwriting each other
- **Audit ledger** — every movement is recorded as an immutable transaction row, including
  both legs of a transfer
- **RFC 7807 errors** — failures are returned as `application/problem+json`, with field-level
  detail for validation errors
- **Cache-optional** — Redis accelerates idempotency lookups but is never the source of
  truth; the service runs correctly, just slower, without it

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation, Actuator) |
| Database | H2, in-memory, PostgreSQL compatibility mode |
| Cache | Redis 7 (optional accelerator, not a dependency for correctness) |
| Mapping | MapStruct |
| API docs | springdoc-openapi / Swagger UI |
| Build | Maven Wrapper |
| Quality | Spotless (google-java-format), JaCoCo, SonarQube |

## Getting started

### Prerequisites

- Java 21
- Docker (used by the test suite for a Redis Testcontainer, and by `docker compose`)

### Run with Docker Compose

Builds the JAR, then starts the service and Redis together:

```bash
./mvnw clean package
docker compose up --build
```

| | |
|---|---|
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health | `http://localhost:8080/actuator/health` |

### Run locally

Needs a Redis instance reachable on `localhost:6379` (or set `CACHE_TYPE=none` to skip
caching entirely):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile switches to plain-text logs and exposes the H2 console at
`/h2-console`.

> [!NOTE]
> The database is in-memory, so every restart starts empty — there's nothing to migrate
> or seed.

## Using the API

```bash
# Create a wallet — 201 Created
curl -X POST http://localhost:8080/v1/wallets \
  -H 'Content-Type: application/json' \
  -d '{"userId":"3f2504e0-4f89-11d3-9a0c-0305e82c3301"}'
# {"walletId":"6163fb26-3a06-4080-a987-35c5e5a17297","balance":"0.00","createdAt":"..."}

# Deposit — 204 No Content, Idempotent-Replayed: false
curl -i -X POST http://localhost:8080/v1/wallets/$WALLET_ID/deposits \
  -H 'Content-Type: application/json' \
  -H 'Correlation-Id: 8f14e45f-1d6f-4f1a-9a3e-0e1c2f4b5a6d' \
  -d '{"amount":"100.00"}'

# Transfer between two wallets — 204 No Content
curl -i -X POST http://localhost:8080/v1/transfers \
  -H 'Content-Type: application/json' \
  -H 'Correlation-Id: 2b6b1f0e-7c3a-4b2d-8f5e-1a9c6d4e3f21' \
  -d '{"fromWalletId":"'$FROM'","toWalletId":"'$TO'","amount":"25.00"}'

# Balance — 200 OK
curl http://localhost:8080/v1/wallets/$WALLET_ID/balance
# {"balance":"75.00"}
```

Amounts are always JSON strings, never numbers — this avoids float rounding on the
client. The direction of the movement (credit or debit) comes from the endpoint, not the
payload.

### Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/v1/wallets` | Create a wallet for a user |
| `GET` | `/v1/wallets/{walletId}/balance` | Read the current balance |
| `POST` | `/v1/wallets/{walletId}/deposits` | Credit a wallet |
| `POST` | `/v1/wallets/{walletId}/withdrawals` | Debit a wallet |
| `POST` | `/v1/transfers` | Move funds between two wallets |

All three mutating endpoints require a `Correlation-Id` header and return `204 No
Content` with an `Idempotent-Replayed` header (`true`/`false`).

### Idempotency

> [!IMPORTANT]
> A replay always answers `204 No Content` — never a failure. A client that lost the
> original response must be able to retry blindly. `Idempotent-Replayed` is the only
> signal that distinguishes a replay from the call that actually moved money.

Replaying the deposit above with the same `Correlation-Id` and the same body is a no-op:
the balance is credited once, no matter how many times the request arrives.

```bash
curl -i -X POST http://localhost:8080/v1/wallets/$WALLET_ID/deposits \
  -H 'Content-Type: application/json' \
  -H 'Correlation-Id: 8f14e45f-1d6f-4f1a-9a3e-0e1c2f4b5a6d' \
  -d '{"amount":"100.00"}'
# HTTP/1.1 204
# Idempotent-Replayed: true
```

Reusing the same `Correlation-Id` with a *different* amount is rejected with `409
Conflict` — the same key can't silently mean two different operations.

### Concurrency

Wallet balances use optimistic locking. Two concurrent requests against the same wallet
race; the loser gets `409 Conflict` and can retry with the *same* `Correlation-Id` —
that's what makes the retry safe rather than a risk of double-processing.

### Errors

Failures are `application/problem+json`, per RFC 7807:

```json
{
  "type": "about:blank",
  "title": "Business violation",
  "status": 404,
  "detail": "Wallet not found",
  "instance": "/v1/wallets/55e476d1-f217-4583-a75a-0dd0a548c858/deposits"
}
```

`title` is `Business violation` for domain failures and `Validation error` for invalid
payloads or missing headers; validation failures also carry an `errors` array of
`{field, message}` entries.

| Status | When |
|---|---|
| `400 Bad Request` | Invalid payload, missing `Correlation-Id`, transfer to the same wallet |
| `404 Not Found` | Wallet does not exist |
| `409 Conflict` | `Correlation-Id` reused with different parameters, or a concurrent update lost the optimistic lock |
| `422 Unprocessable Entity` | Insufficient balance |

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | *(none)* | `dev` for plain-text logs and the H2 console |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `CACHE_TYPE` | `redis` | Set to `none` to disable caching entirely |

Redis is an accelerator for idempotency lookups only — the database is always the source
of truth, and a Redis outage degrades to slower, not incorrect, behavior.

## Testing

```bash
./mvnw test
```

> [!IMPORTANT]
> Docker must be running — the integration suite starts a real Redis container per test
> class.

Every controller has a full-stack integration test against a real Redis and an
in-memory database, both reset before each test.

## Development

```bash
./mvnw spotless:apply     # format
./mvnw verify              # test + format check
```

A pre-commit hook formats staged files automatically; enable it once per clone:

```bash
git config core.hooksPath .githooks
```

Project conventions (architecture, error handling, testing rules, style) live in
[`.claude/rules/code-conventions.md`](.claude/rules/code-conventions.md).

## CI/CD

`.github/workflows/ci.yml` runs `clean verify sonar:sonar` on every pull request to
`main` and on pushes to it, and blocks the merge on a red SonarCloud quality gate.

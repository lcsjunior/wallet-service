# Wallet Service

RESTful microservice for wallet management — supports deposits, withdrawals, and transfers between users.

> Programming challenge. Java 21 + Spring Boot 3.5.

## Functional Requirements

- **Create Wallet** — open a new wallet for a user (a user may own more than one wallet)
- **Retrieve Balance** — get the current balance of a wallet, identified by its own `walletId`
- **Deposit Funds** — add money to a wallet
- **Withdraw Funds** — remove money from a wallet
- **Transfer Funds** — move money between wallets

## Non-Functional Requirements

- **Idempotency** — mutation operations (deposit, withdraw, transfer) are idempotent via a `Correlation-Id` header; retrying the same request produces the same result
- **Concurrency control** — wallet balance updates use optimistic locking (JPA `@Version`); concurrent updates to the same wallet return `409 Conflict`, and the client can safely retry with the same `Correlation-Id`
- **Mission-critical reliability** — downtime directly impairs platform operations
- **Full traceability** — all operations must be auditable for balance reconciliation

## Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 (Web, Data JPA, Actuator) |
| Database | H2 (relational, embedded, file-based at `./data/`) — source of truth |
| Cache | Redis — accelerates idempotency retries only, non-authoritative, optional (`CACHE_TYPE=none`) |
| Mapping | MapStruct |
| API Docs | SpringDoc OpenAPI (`/swagger-ui.html`) |
| Logging | Logback; ECS JSON on the console, plain text under the `dev` profile |
| Build | Maven Wrapper |
| Quality | Spotless, JaCoCo, SonarQube |

## Quick Start

Starts the service and its Redis together — the image packages the built JAR, so the
build step is required:

```bash
./mvnw clean package && docker compose up --build
```

| | |
|---|---|
| Service | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health | `http://localhost:8080/actuator/health` |

H2 is file-based under `./data/`, so balances survive a restart. Delete that folder to
start from a clean database.

Redis is optional — `CACHE_TYPE=none` runs the app entirely off the database. It is also
optional at runtime: a Redis that dies is logged at `WARN` and the request falls through
to the database, so nothing breaks and idempotency still holds.

## API Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/v1/wallets` | Create a wallet for a user, returns the generated `walletId` |
| `GET` | `/v1/wallets/{walletId}/balance` | Current balance of the wallet |
| `POST` | `/v1/wallets/{walletId}/deposits` | Deposit funds (requires `Correlation-Id` header); returns `204 No Content` |
| `POST` | `/v1/wallets/{walletId}/withdrawals` | Withdraw funds (requires `Correlation-Id` header); returns `204 No Content` |
| `POST` | `/v1/transfers` | Transfer funds between two wallets (requires `Correlation-Id` header); returns `204 No Content` |

## Usage

```bash
# 1. Create a wallet — 201 Created
curl -X POST http://localhost:8080/v1/wallets \
  -H 'Content-Type: application/json' \
  -d '{"userId":"3f2504e0-4f89-11d3-9a0c-0305e82c3301"}'

# {"walletId":"6163fb26-3a06-4080-a987-35c5e5a17297","balance":"0.00","createdAt":"..."}

# 2. Deposit — 204 No Content
curl -X POST http://localhost:8080/v1/wallets/$WALLET_ID/deposits \
  -H 'Content-Type: application/json' \
  -H 'Correlation-Id: dep-1' \
  -d '{"amount":"100.00"}'

# 3. Transfer — 204 No Content
curl -X POST http://localhost:8080/v1/transfers \
  -H 'Content-Type: application/json' \
  -H 'Correlation-Id: tf-1' \
  -d '{"fromWalletId":"'$FROM'","toWalletId":"'$TO'","amount":"25.00"}'

# 4. Balance — 200 OK
curl http://localhost:8080/v1/wallets/$WALLET_ID/balance
# {"balance":"75.00"}
```

Amounts are JSON strings with at most 2 decimal places and must be strictly
positive; the direction of the movement comes from the endpoint, never from the
sign.

### Idempotency in practice

Replaying step 2 with `Correlation-Id: dep-1` and the same body is a no-op — the
balance is credited once, no matter how many times the request arrives. Reusing
`dep-1` with a *different* amount is rejected with `409 Conflict`, since the same
key would otherwise mean two different operations.

### Errors

Failures are returned as RFC 7807 `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Business violation",
  "status": 404,
  "detail": "Wallet not found",
  "instance": "/v1/wallets/55e476d1-f217-4583-a75a-0dd0a548c858/deposits"
}
```

The `title` names the kind of failure, not the specific error: `Business violation`
for business failures, `Validation error` for invalid payloads and missing headers.
Validation failures add an `errors` array of `{field, message}` entries.

| Status | When |
|--------|------|
| `400 Bad Request` | Invalid payload, missing `Correlation-Id`, transfer to the same wallet |
| `404 Not Found` | Wallet does not exist |
| `409 Conflict` | `Correlation-Id` reused with different parameters, or a concurrent update lost the optimistic lock — retry is safe |
| `422 Unprocessable Entity` | Insufficient balance |

## Tests

Integration tests run the full stack against an in-memory H2 and a Testcontainers Redis,
so **Docker must be running**.

## CI/CD

- **Automatic formatting**: a versioned pre-commit hook formats the code before every commit. Enable it once per clone with `git config core.hooksPath .githooks`.
- **Pull requests to `main`**: `.github/workflows/ci.yml` runs one job, `code-quality`: `clean verify sonar:sonar` on `ubuntu-24.04`. A red SonarCloud quality gate fails it and blocks the merge.
- **Settings on GitHub, not in the repo**: the `SONAR_TOKEN` secret, and `Code Quality` as a required check under **Settings → Branches**. The SonarCloud organization and project key are derived from the repository slug, so the workflow is portable as is.
- **On SonarCloud**: the project must exist and *Automatic Analysis* must be off, otherwise the CI-based analysis is rejected.


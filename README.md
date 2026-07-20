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

- **Idempotency** — mutation operations (deposit, withdraw, transfer) are idempotent via a `correlationId` header; retrying the same request produces the same result
- **Mission-critical reliability** — downtime directly impairs platform operations
- **Full traceability** — all operations must be auditable for balance reconciliation

## Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 (Web, Data JPA, Actuator) |
| Database | H2 (relational, embedded) — source of truth |
| Cache | Redis (idempotency retries only, non-authoritative); declarative via Spring Cache (`@Cacheable`/`@CachePut`), configured entirely through `spring.cache.type` (`redis`/`none`) and `spring.cache.redis.time-to-live` |
| Mapping | MapStruct |
| API Docs | SpringDoc OpenAPI (`/swagger-ui.html`) |
| Build | Maven Wrapper |
| Quality | Spotless, JaCoCo, SonarQube |

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

## CI/CD

- **Automatic formatting**: the repository versions a pre-commit hook at `.githooks/pre-commit` that runs `./mvnw spotless:apply` before every commit. Enable it once per clone with:
  ```bash
  git config core.hooksPath .githooks
  ```
- **Pull requests to `main`**: `.github/workflows/ci.yml` runs two jobs on every PR — `test` (`./mvnw clean test` with a JaCoCo report) and `sonar` (static analysis via `sonar-maven-plugin`, using the `SONAR_TOKEN`/`SONAR_HOST_URL` secrets). To ensure no change is merged without passing both, manually configure the `Test (JaCoCo)` and `Static Analysis (SonarQube)` checks as **required status checks** under **Settings → Branches → Branch protection rules** for `main` — this is a GitHub setting, not a versionable file in the repository.

## API Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/v1/wallets` | Create a wallet for a user, returns the generated `walletId` |
| `GET` | `/v1/wallets/{walletId}/balance` | Current balance of the wallet |
| `POST` | `/v1/wallets/{walletId}/deposits` | Deposit funds (requires `Correlation-Id` header); returns `204 No Content` |
| `POST` | `/v1/wallets/{walletId}/withdrawals` | Withdraw funds (requires `Correlation-Id` header); returns `204 No Content` |
| `POST` | `/v1/transfers` | Transfer funds between two wallets (requires `Correlation-Id` header); returns `204 No Content` |

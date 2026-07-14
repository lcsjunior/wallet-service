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

## Design Decisions

*(to be completed — trade-offs and choices made during implementation)*

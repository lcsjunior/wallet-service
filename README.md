# Wallet Service

[![CI](https://github.com/lcsjunior/wallet-service/actions/workflows/ci.yml/badge.svg)](https://github.com/lcsjunior/wallet-service/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=lcsjunior_wallet-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=lcsjunior_wallet-service)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=lcsjunior_wallet-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=lcsjunior_wallet-service)

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)](https://spring.io/projects/spring-boot)

A RESTful microservice that manages wallets: deposits, withdrawals, and transfers between
users. Programming challenge, not a production payments system — in-memory database,
single node.

## Features

- Wallets — a user may hold more than one, so a repeated `Idempotency-Key` is rejected, never
  silently duplicated
- Deposits, withdrawals, and transfers between two wallets
- Every mutation is guarded by an `Idempotency-Key`: a unique constraint in the database answers
  `409` to a repeat, so nothing is ever applied twice
- Optimistic locking — concurrent updates to the same wallet fail with `409`
- Immutable audit ledger, with both legs of a transfer recorded
- RFC 9457 errors (`application/problem+json`)

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation, Actuator) |
| Database | H2, in-memory, PostgreSQL compatibility mode |
| Mapping | MapStruct |
| API docs | springdoc-openapi / Swagger UI |
| Build | Maven Wrapper |
| Quality | Spotless (google-java-format), JaCoCo, SonarQube |

## Getting started

Requires Java 21; Docker only for `docker compose`.

```bash
./mvnw clean package
docker compose up --build
```

| | |
|---|---|
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health | `http://localhost:8080/actuator/health` |

Or locally:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile adds SQL logging and the H2 console at `/h2-console`. The database
is in-memory: every restart starts empty.

Application logs carry the request's `Idempotency-Key` and its optional `X-Correlation-ID` as the
MDC fields `idempotencyKey` and `correlationId`; HTTP payloads are never logged.

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/v1/wallets` | Create a wallet for a user |
| `POST` | `/v1/wallets/{walletId}/deposits` | Credit a wallet |
| `POST` | `/v1/wallets/{walletId}/withdrawals` | Debit a wallet |
| `POST` | `/v1/transfers` | Move funds between two wallets |

```bash
# Create a wallet — 201 Created
curl -X POST http://localhost:8080/v1/wallets \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 6c1b0a2d-5e4f-4c3b-8a19-7d2e3f4a5b60' \
  -H 'X-Correlation-ID: abc123def456-trace-999' \
  -d '{"userId":"3f2504e0-4f89-11d3-9a0c-0305e82c3301"}'
# {"walletId":"6163fb26-3a06-4080-a987-35c5e5a17297","createdAt":"..."}

# Deposit — 204 No Content
curl -i -X POST http://localhost:8080/v1/wallets/$WALLET_ID/deposits \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 8f14e45f-1d6f-4f1a-9a3e-0e1c2f4b5a6d' \
  -d '{"amount":"100.00"}'

# Transfer between two wallets — 204 No Content
curl -i -X POST http://localhost:8080/v1/transfers \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 2b6b1f0e-7c3a-4b2d-8f5e-1a9c6d4e3f21' \
  -d '{"fromWalletId":"'$FROM'","toWalletId":"'$TO'","amount":"25.00"}'
```

Amounts are JSON strings, never numbers. Every endpoint requires `Idempotency-Key`, a UUID, and
reusing one answers `409` rather than applying the request twice — there is no silent replay.
`X-Correlation-ID` is optional and free-form: it never changes the outcome, it only tags the log
lines of the request. The money movements answer `204 No Content` with no body; wallet creation
answers `201 Created` with the wallet.

### Errors

```json
{
  "type": "about:blank",
  "title": "Business violation",
  "status": 404,
  "detail": "Wallet not found",
  "instance": "/v1/wallets/55e476d1-f217-4583-a75a-0dd0a548c858/deposits"
}
```

| Status | When |
|---|---|
| `400 Bad Request` | Invalid payload, missing `Idempotency-Key`, transfer to the same wallet |
| `404 Not Found` | Wallet does not exist |
| `409 Conflict` | `Idempotency-Key` already used on that wallet, or a concurrent update lost the optimistic lock |
| `422 Unprocessable Entity` | Insufficient balance |

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | *(none)* | `dev` for SQL logging and the H2 console |

## Development

```bash
./mvnw test              # run the suite
./mvnw verify            # test + format check
./mvnw spotless:apply    # format

git config core.hooksPath .githooks   # once per clone, formats staged files
```

Conventions live in
[`.claude/rules/code-conventions.md`](.claude/rules/code-conventions.md).

## CI/CD

Pull requests to `main` must clear three gates before merging:

| Workflow | Gate |
|---|---|
| `ci.yml` | `clean verify sonar:sonar` — a red SonarCloud quality gate blocks the merge |
| `sast.yml` | CodeQL static analysis, `security-extended` query suite |
| `sca.yml` | Dependency review — fails on high or critical advisories in added dependencies |

Dependabot opens version-update pull requests weekly for Maven, GitHub Actions and Docker,
and security updates as advisories land. To report a vulnerability, see
[`SECURITY.md`](SECURITY.md).

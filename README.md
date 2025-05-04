# Digital Wallet Service

A microservice responsible for managing user wallets. It supports operations such as deposits, withdrawals, and transfers while ensuring auditability and historical balance tracking.

*This project was developed as part of a coding challenge for a developer position.*

---

## Features

- [x] Create wallets
- [x] Deposit and withdraw funds
- [x] Transfer funds between wallets
- [x] Retrieve current and historical balances
- [x] Full operation traceability for audit compliance

---

## Tech Stack

- [x] Java 17
- [x] Quarkus Framework
- [x] Hibernate ORM and Panache
- [x] H2 Database
- [x] JUnit 5, Mockito and AssertJ
- [x] REST API, OpenAPI and Swagger

---

## How to Run

### 1. Clone the repository
```bash
git clone https://github.com/lcsjunior/wallet-service.git
cd wallet-service
```

### 2. Running the application in dev mode
```bash
./mvnw quarkus:dev
```

The service will start on `http://localhost:8080`

**Swagger UI available at:** http://localhost:8080/q/swagger-ui/

### 3. Running unit and integration tests
```bash
./mvnw clean test
```

---

## API Endpoints

| Method | Endpoint                 | Description                       |
|--------|--------------------------|-----------------------------------|
| POST   | `/wallets`               | Create a new wallet               |
| GET    | `/wallets/{id}/balance`  | Get current wallet balance        |
| GET    | `/wallets/{id}/balance?at=timestamp` | Get historical balance at specific time |
| POST   | `/wallets/deposit`  | Deposit funds                     |
| POST   | `/wallets/withdraw` | Withdraw funds                    |
| POST   | `/wallets/transfer` | Transfer funds between wallets    |

> All operations accept and return JSON.

---

## Design Decisions

- **Scalability**: Quarkus was chosen for its native compatibility with Kubernetes and Serverless, offering fast initialization and efficient resource consumption.
- **Availability**: Independent microservice with fault isolation, ensuring resilience and high availability.
- **Traceability**: Each transaction is persisted in the Transaction table, containing timestamp, wallet, amount, operation type, and related transaction, facilitating auditing and diagnostics.
- **Testability**: Extensive automated test coverage, covering core business rules, transaction validations, and error scenarios (such as insufficient balance).
- **Integrity**: Ensured with ACID transactions, concurrency control with @Version (optimistic locking), and balance update via native SQL to avoid race conditions and consistency.

---

## Trade-offs

- **Idempotency of operations**: Idempotency is not implemented, which may cause issues with retries. It is planned for future evolution.
- **User-wallet relationship**: The wallet has a userId field, but the relationship has not been implemented. Currently, the userId field is stored as NIL_UUID.
- **Use of H2 in environments**: H2 is used for testing, with migration to PostgreSQL planned for production and staging environments.
- **Test coverage**: Focus on functional tests; unit, performance, and security test coverage needs to be expanded.
- **Code quality with SonarQube**: The project uses Jacoco for code coverage analysis, but improvements in code quality metrics will be explored in the future.
- **Authentication and authorization**: Authentication and authorization have not been implemented. The plan is to use Cognito for these features in the future.
- **CI/CD with GitHub Actions**: The pipeline could not be implemented due to time constraints, but it is planned for future implementation.
- **Logs and observability**: Logs have been improved with OpenTelemetry, but coverage and visibility need to be expanded.
- **Messaging**: There is no messaging system in place at the moment, but it will be evaluated based on scalability and decoupling needs.


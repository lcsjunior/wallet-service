# Digital Wallet Service

A microservice responsible for managing user wallets. It supports operations such as deposits, withdrawals, and transfers while ensuring auditability and historical balance tracking.

*This project was developed as part of a coding challenge for a developer position.*

---

## Features

- ✅ Create wallets
- ✅ Deposit and withdraw funds
- ✅ Transfer funds between wallets
- ✅ Retrieve current and historical balances
- ✅ Full operation traceability for audit compliance

---

## Tech Stack

- Java 17
- Quarkus Framework
- Hibernate ORM and Panache
- H2 Database
- JUnit 5, Mockito and AssertJ
- REST API, OpenAPI and Swagger

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

The service will start on `http://localhost:8081`
> Take a look at **Swagger:** http://localhost:8081/q/swagger-ui/

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

- **Traceability**: Each transaction is persisted with timestamp, operation type, and amount, ensuring a complete audit trail.
- **Separation of concerns**: Wallets and transactions are modeled independently, enabling clear domain boundaries and easier testing.
- **In-memory H2 DB**: Used to simplify setup and ensure fast feedback during testing.

---

## Trade-offs

- **Traceability**: Each transaction is persisted with timestamp, operation type, and amount, ensuring a complete audit trail.
- **Separation of concerns**: Wallets and transactions are modeled independently, enabling clear domain boundaries and easier testing.
- **In-memory H2 DB**: Used to simplify setup and ensure fast feedback during testing.


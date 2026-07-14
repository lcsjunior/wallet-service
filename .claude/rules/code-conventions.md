# Code Conventions

## Architecture

- Layered MVC: controller → service → repository
- Immutable DTOs via Java `record`
- Entity ↔ DTO mapping with MapStruct
- Object construction via factory methods or builders (no direct `new`)
- All new code goes under `src/main/java/com/example/wallet_service/` following the same layering

```
src/main/java/com/example/wallet_service/
├── controller/     # REST controllers
├── dto/            # Immutable request/response records
├── entity/         # JPA entities
├── mapper/         # MapStruct mappers (Entity ↔ DTO)
├── repository/     # Spring Data JPA repositories
├── service/        # Business logic
└── WalletServiceApplication.java
```

## Package Naming

Packaged as `com.example.wallet_service` (note underscore — the artifact name `wallet-service` is invalid as a Java package name).

## Testing

- Every controller must have an integration test (`@WebMvcTest` or `@SpringBootTest`)
- Tests assert the full JSON response using **strict** mock JSON (`.content().json(expectedJson, true)`) — no extra or missing fields allowed

### Method Naming

- Every test method name must start with `should` (e.g., `shouldReturnBalanceWhenWalletExists`)
- Every test method must be annotated with `@DisplayName` describing the scenario

## Docker

The Docker image is built from a pre-compiled JAR (`target/wallet-service-0.0.1-SNAPSHOT.jar`) using `eclipse-temurin:21-jre-alpine`. Run `./mvnw clean package` before `docker compose up`.

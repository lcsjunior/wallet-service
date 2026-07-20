# Code Conventions

## Architecture

- Layered MVC: controller → service → repository
- Immutable DTOs via Java `record`
- Entity ↔ DTO mapping with MapStruct
- Object construction via factory methods or builders (no direct `new`)
- Every repository must be an interface; concrete repository implementation classes must be suffixed `Impl` (e.g., `WalletRepository` → `WalletRepositoryImpl`)
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

## Testing

- Every controller must have an integration test (`@WebMvcTest` or `@SpringBootTest`)
- Integration test classes MUST be suffixed `IntegrationTest` (e.g., `WalletControllerIntegrationTest`); unit test classes MUST be suffixed `Test` (e.g., `WalletServiceTest`)
- Tests assert the full JSON response using **strict** mock JSON (`.content().json(expectedJson, true)`) — no extra or missing fields allowed
- Expected/mock JSON payloads used in integration tests MUST be created in dedicated `.json` files under `src/test/resources/mock/json/`, never as inline Java string literals
- Unit tests (service layer) MUST use Mockito (`@Mock`/`@InjectMocks` or `Mockito.mock`) to isolate the unit under test from its collaborators (repositories, other services)
- Unit tests MUST call `Mockito.verify(...)` whenever the test's purpose is to confirm a collaborator was (or was not) invoked with specific arguments — e.g., confirming a repository save happened exactly once, or that a second call with the same `correlationId` never reaches the persistence layer. Skip `verify()` only when the assertion is purely on the returned value/state and no interaction needs confirming.

## Package Naming

Packaged as `com.example.wallet_service` (note underscore — the artifact name `wallet-service` is invalid as a Java package name).

## Monetary Values

- Every monetary value MUST be represented as `BigDecimal`, scale exactly 2 (e.g., `0.01`) — never `double`/`float`.
- Values with more than 2 decimal places (e.g., `0.015`) MUST be rejected as a validation error at the system boundary (DTO validation) — never silently rounded or truncated to 2 places.
- Amount fields on request DTOs (deposit/withdrawal/transfer) MUST be strictly positive (e.g., Bean Validation `@Positive`); zero and negative values MUST be rejected as a validation error at the DTO boundary, never reaching the service layer. Sign of the effect on balance comes from the operation type, not from the stored value.

## Method Naming

- Method names MUST be objective, short, and free of abbreviations — this applies to production code and test code alike
- No method name may exceed 38 characters
- Factory methods MUST be named `of` (e.g., `Wallet.of(userId)`) — never `create`, `createNew`, or similar
- Every test method name must start with `should` (e.g., `shouldReturnBalanceWhenWalletExists`)
- Every test method must be annotated with `@DisplayName` describing the scenario

## Local Variables

- Prefer `var` over explicit types for local variable declarations when the type is clear from the right-hand side.

## Control Flow

- Prefer early returns (guard clauses) over nested conditionals — return/throw as soon as a precondition fails instead of wrapping the remaining logic in an `else` block.
- Prefer method references over lambdas whenever possible (e.g., `list.forEach(this::process)` over `list.forEach(item -> process(item))`).

## Logging

- Every class that logs MUST declare a `LOG_PREFIX` constant and prepend it to every log message, e.g.:

```java
private static final String LOG_PREFIX = "[WALLET_SERVICE] ";

log.info(LOG_PREFIX + "Wallet created | userId={}, walletId={}", newWallet.getUserId(), newWallet.getId());
```

## Comments

- Code comments and Javadoc are forbidden anywhere in this codebase — no `//`, `/* */`, or `/** */` blocks. Code must be self-explanatory through naming and structure.

## Docker

The Docker image is built from a pre-compiled JAR (`target/wallet-service-0.0.1-SNAPSHOT.jar`) using `eclipse-temurin:21-jre-alpine`. Run `./mvnw clean package` before `docker compose up`.

## Project documentation

- `README.md`, `CLAUDE.md`, and `AGENTS.md` must always be kept up to date.
- Whenever a change affects architecture, conventions, hardware wiring, dependencies,
  or any information already documented in these files, update them in the same
  change set.

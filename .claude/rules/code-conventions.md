# Code Conventions

## Architecture

- Layered MVC: controller → service → repository
- Every controller must be mapped under a `/v1/...` prefix (e.g., `/v1/wallets`)
- Immutable DTOs via Java `record`
- Entity ↔ DTO mapping with MapStruct
- DTOs MUST only be transformed to/from entities (or other DTOs) via a MapStruct mapper — never manually field-by-field in a controller, service, or elsewhere
- Object construction via factory methods or builders (no direct `new`)
- Every repository must be an interface; concrete repository implementation classes must be suffixed `Impl` (e.g., `WalletRepository` → `WalletRepositoryImpl`)
- All new code goes under `src/main/java/com/example/wallet/` following the same layering

```
src/main/java/com/example/wallet/
├── config/         # Spring configuration (e.g., @EnableCaching)
├── controller/     # REST controllers
├── dto/            # Immutable request/response records
├── entity/         # JPA entities
├── exception/      # ServiceException, ErrorCode, global handler
├── mapper/         # MapStruct mappers (Entity ↔ DTO)
├── repository/     # Spring Data JPA repositories
├── service/        # Business logic
│   └── resolver/   # Cross-cutting service helpers (e.g., message resolution)
└── WalletApplication.java
```

## Testing

- Every controller must have an integration test (`@WebMvcTest` or `@SpringBootTest`)
- Integration tests MUST NOT use mocks (e.g., `@MockitoBean`) — they must exercise the full stack, not stub out collaborators like `TransactionService`
- Integration test classes MUST be suffixed `IntegrationTest` (e.g., `WalletControllerIntegrationTest`); unit test classes MUST be suffixed `Test` (e.g., `WalletServiceTest`)
- Tests assert the full JSON response using **strict** mock JSON (`.content().json(expectedJson, true)`) — no extra or missing fields allowed
- Expected/mock JSON payloads used in integration tests MUST be created in dedicated `.json` files under `src/test/resources/mock/json/`, never as inline Java string literals
- Unit tests (service layer) MUST use Mockito (`@Mock`/`@InjectMocks` or `Mockito.mock`) to isolate the unit under test from its collaborators (repositories, other services)
- Unit tests MUST call `Mockito.verify(...)` whenever the test's purpose is to confirm a collaborator was (or was not) invoked with specific arguments — e.g., confirming a repository save happened exactly once, or that a second call with the same `correlationId` never reaches the persistence layer. Skip `verify()` only when the assertion is purely on the returned value/state and no interaction needs confirming.

## Package Naming

Packaged as `com.example.wallet`.

## Exceptions

- Business errors MUST be represented by throwing `ServiceException` — never create a dedicated exception subclass per error case. `ServiceException` holds exactly two attributes, `message` (inherited from `Throwable`) and `httpStatus`, and exposes exactly two public constructors: `ServiceException(String message, HttpStatus httpStatus)`, which sets both attributes directly, and `ServiceException(ErrorCode errorCode)`, a convenience overload that decomposes the enum and delegates to the first constructor (e.g., `new ServiceException(WALLET_NOT_FOUND)`).
- The error JSON response (`ProblemDetail`) exposes only `title`, `detail`, `status`, `instance`, and — for validation errors — `errors`. It MUST NOT include a machine-readable error `code` property.

## Message Keys

- Keys in `messages.properties` MUST NOT contain hyphens; compound words within a segment are concatenated (e.g., `wallet.notfound`, `correlationid.conflict`, `transfer.samewallet`) — never `wallet.not-found` or `correlation-id.conflict`.
- Delete unused message keys in the same change set that removes their last reference.
- Entries in `messages.properties` MUST be sorted alphabetically by key.
- Message values MUST NOT end with punctuation (no trailing `.`, `!`, etc.) — e.g., `wallet.notfound=Wallet not found`, never `Wallet not found.`.

## Monetary Values

- Every monetary value MUST be represented as `BigDecimal`, scale exactly 2 (e.g., `0.01`) — never `double`/`float`.
- Values with more than 2 decimal places (e.g., `0.015`) MUST be rejected as a validation error at the system boundary (DTO validation) — never silently rounded or truncated to 2 places.
- Amount fields on request DTOs (deposit/withdrawal/transfer) MUST be strictly positive (e.g., Bean Validation `@Positive`); zero and negative values MUST be rejected as a validation error at the DTO boundary, never reaching the service layer. Sign of the effect on balance comes from the operation type, not from the stored value.

## Caching

- Caching MUST be implemented exclusively via `@Cacheable` — `@CachePut`, `@CacheEvict`, and manual `CacheManager`/`Cache` API usage are prohibited. Service classes MUST NOT contain any cache-specific code (no manual cache population, no cache-key handling); services only call the repository's `find`/`save` methods, and caching happens as a side effect of `@Cacheable` on the repository.
- A `@Cacheable` method MUST NOT cache a missing/absent result — apply `unless = "#result == null"` (Spring unwraps `Optional`-returning methods before evaluating `unless`, so `#result` is the unwrapped value, never the `Optional` itself).
- All cache configuration (cache names' backing settings, TTL, provider setup) MUST live in `src/main/resources/application.properties` and/or `src/test/resources/application*.properties` — never hard-coded in Java classes.

## Method Naming

- Method names MUST be objective, short, and free of abbreviations — this applies to production code and test code alike
- No method name may exceed 38 characters
- Factory methods MUST be named `of` (e.g., `Wallet.of(userId)`) — never `create`, `createNew`, or similar
- A method named `validate*` MUST contain a conditional that decides whether to reject the input (e.g., `if (...) throw ...`) — a method that unconditionally builds/logs/throws has nothing to validate and MUST be named after what it does instead (e.g., a method that always logs and returns a `ServiceException` for a missing wallet is `walletNotFoundException`, not `validateWalletExists`)
- Every test method name must start with `should` (e.g., `shouldReturnBalanceWhenWalletExists`)
- Every test method must be annotated with `@DisplayName` describing the scenario
- A method whose returned object is never consumed by any caller MUST be declared `void` — do not return a value nobody reads

## Local Variables

- Prefer `var` over explicit types for local variable declarations when the type is clear from the right-hand side.

## Imports

- Static constants and enum values (e.g., `ErrorCode.WALLET_NOT_FOUND`, `HttpStatus.CREATED`, `EnumType.STRING`, `BigDecimal.ZERO`) MUST be brought in with `import static` and referenced unqualified — never qualified with the declaring type at the call site.
- Skip the static import (keep the reference qualified) only when it would collide with another name already used unqualified in the same file — e.g., `OperationType.DEPOSIT` and `TransactionType.DEPOSIT` both exist, so when a class uses both enums, leave every reference to them qualified rather than statically importing just one.
- This rule does not apply to ordinary static factory method calls (e.g., `Optional.of(...)`, `UUID.randomUUID()`, `ResponseEntity.ok(...)`, `LoggerFactory.getLogger(...)`) or to this project's own `of(...)` factory methods — those stay qualified with the class name.

## Control Flow

- Prefer early returns (guard clauses) over nested conditionals — return/throw as soon as a precondition fails instead of wrapping the remaining logic in an `else` block.
- Prefer method references over lambdas whenever possible (e.g., `list.forEach(this::process)` over `list.forEach(item -> process(item))`).

## Logging

- Every class that logs MUST declare a `LOG_PREFIX` constant and prepend it to every log message. `LOG_PREFIX` MUST be the declaring class's own name in `UPPER_SNAKE_CASE` (e.g., `TransactionService` → `[TRANSACTION_SERVICE] `) — never copied from another class, e.g.:

```java
// inside TransactionService
private static final String LOG_PREFIX = "[TRANSACTION_SERVICE] ";

log.info(LOG_PREFIX + "Wallet created | userId={}, walletId={}", newWallet.getUserId(), newWallet.getId());
```

## Comments

- Code comments and Javadoc are forbidden anywhere in this codebase — no `//`, `/* */`, or `/** */` blocks. Code must be self-explanatory through naming and structure.

## Docker

The Docker image is built from a pre-compiled JAR (`target/wallet-service-0.0.1-SNAPSHOT.jar`) using `eclipse-temurin:21-jre-alpine`. Run `./mvnw clean package` before `docker compose up`.

## Project documentation

- `README.md` and `CLAUDE.md` must always be kept up to date.
- Whenever a change affects architecture, conventions, hardware wiring, dependencies,
  or any information already documented in these files, update them in the same
  change set.
- `README.md` MUST be written entirely in English — no other language, in any section.

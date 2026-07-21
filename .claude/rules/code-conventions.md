# Code Conventions

## Architecture

- Layered MVC: controller → service → repository, all under `com.example.wallet`
- Controllers mapped under `/v1/...`
- DTOs are immutable `record`s; entity ↔ DTO conversion only through a MapStruct mapper, never field-by-field
- Objects are built by factory methods or builders — no direct `new`
- Repositories are Spring Data interfaces extending `JpaRepository`; never write an implementation class

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
│   └── resolver/   # Cross-cutting service helpers
└── WalletApplication.java
```

## Testing

Integration test classes are suffixed `IntegrationTest`, unit test classes `Test`.

**Integration** — every controller has one, exercising the full stack with no mocks:

- Extend `AppTests`, which holds the shared `MockMvc` and `expectBalance`; wallet UUIDs and other scenario data stay in the concrete class
- Assert the whole body with `.content().json(expected, STRICT)` — no extra or missing fields
- Tests of a mutation endpoint end with `expectBalance(...)`, failure cases included: it proves a rejected request had no side effect (a transfer to a missing wallet must not debit the source)
- Payloads with one field or none come from `JsonUtils.fieldJson(field, value)` / `emptyJson()`, keeping the value at the call site
- Larger payloads live in `src/test/resources/mock/json/{request,response}/<controller>/`, named `<payload-type>-<scenario>.json` — payload type is `amount`/`transfer`/`wallet` for requests, `error`/`balance`/`wallet` for responses
- Those files are hard-coded: every UUID and `instance` path written out literally. No `${...}` templating, no runtime substitution — to change an expectation, edit the file
- Seed data is one `.sql` per test class in `mock/sql/`, applied with `@Sql(scripts = "...", executionPhase = BEFORE_TEST_METHOD)`. Each script clears every table, then inserts what the class needs; that reset per method is what buys isolation and order-independence
- Seeded UUIDs are real, written literally, and never repeat across seed files; `user_id` is the same value everywhere

**Unit** — Mockito (`@Mock`/`@InjectMocks`) to isolate the unit from repositories and other services. Call `verify(...)` whenever the point is that a collaborator was (or was not) invoked with given arguments; skip it when asserting only the returned value.

## Naming

- Method names are objective, short, free of abbreviations, and never exceed 38 characters
- Factory methods are named `of` (`Wallet.of(userId)`) — never `create` or similar
- `validate*` must contain a conditional that rejects the input; a method that unconditionally builds or throws is named after what it does (`walletNotFoundException`)
- A method whose return value no caller reads is declared `void`
- Test methods are `should<Outcome>When<Condition>`, annotated with `@DisplayName` reading `Deve retornar <status> [efeito] quando <condição>`

## Exceptions

- Business errors throw `ServiceException` — never a subclass per case. It carries `message` and `httpStatus`, with exactly two constructors: `(String, HttpStatus)` and `(ErrorCode)`, the latter delegating to the former
- The `ProblemDetail` body exposes only `title`, `detail`, `status`, `instance`, plus `errors` for validation failures — never a machine-readable `code`

## Message Keys

- No hyphens; compound words are concatenated (`wallet.notfound`, `correlationid.conflict`)
- Sorted alphabetically, values without trailing punctuation (`Wallet not found`)
- A key is deleted in the same change set that removes its last reference

## Monetary Values

- Always `BigDecimal` with scale exactly 2 — never `double`/`float`
- More than 2 decimal places is a validation error at the DTO boundary, never silently rounded
- Request amounts are strictly positive (`@Positive`); the sign of the effect comes from the operation type, not the value

## Caching

- Only `@Cacheable`, declared on the repository — `@CachePut`, `@CacheEvict` and the `CacheManager` API are prohibited, and services hold no cache-aware code
- Never cache an absent result: `unless = "#result == null"` (Spring unwraps `Optional` before evaluating it)
- All cache settings live in `application.properties`, never hard-coded in Java

## Style

- Prefer `var` when the type is clear from the right-hand side
- Static-import constants and enum values (`WALLET_NOT_FOUND`, `CREATED`, `STRING`) and reference them unqualified; keep them qualified only when two enums would collide in the same file. Ordinary static calls (`Optional.of`, `UUID.randomUUID`, `Wallet.of`) stay qualified
- Guard clauses over nested conditionals; method references over lambdas
- No comments or Javadoc anywhere — naming and structure carry the meaning

## Logging

Every logging class declares `LOG_PREFIX` with its own name in `UPPER_SNAKE_CASE`, prepended to every message:

```java
private static final String LOG_PREFIX = "[TRANSACTION_SERVICE] ";

log.info(LOG_PREFIX + "Wallet created | userId={}, walletId={}", wallet.getUserId(), wallet.getId());
```

## Docker

The image is built from the pre-compiled JAR (`target/wallet-service-0.0.1-SNAPSHOT.jar`) on `eclipse-temurin:21-jre-alpine`. Run `./mvnw clean package` before `docker compose up`.

## Documentation

`README.md` (English only) and `CLAUDE.md` are updated in the same change set as any change to architecture, conventions or dependencies.
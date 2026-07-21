# Code Conventions

## Architecture

- Layered MVC: controller → service → repository, all under `com.example.wallet`
- Controllers mapped under `/v1/...`
- DTOs are immutable `record`s; entity ↔ DTO conversion only through MapStruct
- Objects come from factory methods or builders — no direct `new`
- Repositories are Spring Data interfaces; never write an implementation class

```
src/main/java/com/example/wallet/
├── config/         # Spring configuration
├── controller/     # REST controllers
├── dto/            # Request/response records
├── entity/         # JPA entities
├── exception/      # ServiceException, ErrorCode, global handler
├── mapper/         # MapStruct mappers
├── repository/     # Spring Data JPA repositories
├── service/        # Business logic
│   └── resolver/   # Cross-cutting service helpers
└── WalletApplication.java
```

## Testing

Integration classes are suffixed `IntegrationTest`, unit classes `Test`.

**Integration** — one per controller, full stack, no mocks:

- Extend `AppTests` (shared `MockMvc` and `expectBalance`); wallet UUIDs stay as constants in the concrete class
- Assert the whole body with `.content().json(expected, STRICT)`
- Mutation tests end with `expectBalance(...)`, failures included — it proves no side effect
- Payloads with one field or none: `JsonUtils.fieldJson(field, value)` / `emptyJson()`
- Larger payloads: `JsonUtils.loadJson(path)`, relative to `src/test/resources/mock/json/`, laid out as `{request,response}/<controller>/<payload-type>-<scenario>.json` — payload type names what the body is (`transfer` for a request, `error` for a problem response)
- Those files are hard-coded — every UUID and `instance` path literal, no templating
- One seed `.sql` per test class in `mock/sql/`, applied with a class-level `@Sql(scripts = "...", executionPhase = BEFORE_TEST_METHOD)`; each script clears every table, then inserts what the class needs
- Seeded UUIDs are literal and never repeat across seed files; `user_id` is the same everywhere

**Unit** — Mockito (`@Mock`/`@InjectMocks`). Use `verify(...)` only when the point is that a collaborator was (or was not) called.

## Naming

- Method names are objective, short, unabbreviated, max 38 characters
- Factory methods are named `of` (`Wallet.of(userId)`)
- `validate*` must contain a conditional that rejects the input; otherwise name the method after what it does (`walletNotFoundException`)
- A method whose return value no caller reads is `void`
- Test methods are `should<Outcome>When<Condition>`, with `@DisplayName` reading `Deve retornar <status> [efeito] quando <condição>`

## Exceptions

- Business errors throw `ServiceException` — never a subclass. It carries `message` and `httpStatus`, with exactly two constructors: `(String, HttpStatus)` and `(ErrorCode)`
- The message it carries is a message key, resolved against `messages.properties` by `MessageResolver` when the handler builds the response
- `ProblemDetail` exposes only `title`, `detail`, `status`, `instance`, plus `errors` for validation failures — never a machine-readable `code`
- The `title` comes from the failure kind, not from the specific error: `business.error.title` for business failures, `validation.error.title` for bean validation and missing request headers

## Message Keys

- No hyphens; compound words concatenated (`wallet.notfound`, `correlationid.conflict`)
- Sorted alphabetically, values without trailing punctuation
- A key is deleted with its last reference

## Monetary Values

- Always `BigDecimal` with scale exactly 2 — never `double`/`float`
- More than 2 decimals is a validation error at the DTO boundary, never rounded
- Request amounts are `@Positive`; the sign comes from the operation type

## Caching

- Redis-backed, enabled by `CacheConfig` (`@EnableCaching`)
- Only `@Cacheable`, on the repository — `@CachePut`, `@CacheEvict` and `CacheManager` are prohibited; services hold no cache-aware code
- Writes populate the cache by overriding `save` with `@Cacheable` too — that is deliberate, not a mistake to be "fixed" into `@CachePut`
- Never cache an absent result: `unless = "#result == null"`
- All cache settings (type, TTL, host) live in `application.properties`

## Style

- Prefer `var` when the type is clear from the right-hand side
- Static-import constants and enum values (`WALLET_NOT_FOUND`, `CREATED`) and use them unqualified, unless two enums collide in the file; ordinary static calls (`Optional.of`, `Wallet.of`) stay qualified
- Guard clauses over nested conditionals; method references over lambdas
- No comments or Javadoc anywhere

## Logging

Every logging class declares `LOG_PREFIX` with its own name in `UPPER_SNAKE_CASE`:

```java
private static final String LOG_PREFIX = "[TRANSACTION_SERVICE] ";

log.info(LOG_PREFIX + "Wallet created | userId={}, walletId={}", wallet.getUserId(), wallet.getId());
```

## Docker

The image is built from the pre-compiled JAR (`target/wallet-service-0.0.1-SNAPSHOT.jar`) on `eclipse-temurin:21-jre-alpine`. Run `./mvnw clean package` before `docker compose up`.

## Documentation

`README.md` (English only) and `CLAUDE.md` are updated in the same change set as any change to architecture, conventions or dependencies.
# Code Conventions

This file is never edited without the user's explicit approval — ask first before adding,
changing or removing any rule, and state what the code does today that motivates it.

## Architecture

- Controllers mapped under `/v1/...`
- DTOs are immutable `record`s
- Entity ↔ DTO conversion only through MapStruct
- Repositories are Spring Data interfaces — never an implementation class

## Exceptions

- Business errors throw `ServiceException`, never a subclass
- Its message is a key from `messages.properties`
- `ProblemDetail` carries `title`, `detail`, `status`, `instance`, plus `errors` for validation failures — never a machine-readable `code`
- `title` states the failure kind, not the specific error
- Message keys: no hyphens, sorted alphabetically, no trailing punctuation, deleted with their last reference

## Monetary Values

- `BigDecimal` with scale exactly 2 — never `double`/`float`
- More than 2 decimals is a validation error at the DTO boundary, never rounded
- Request amounts are `@Positive`; the sign comes from the operation type

## Caching

- Cache annotations live only on the repository
- Never cache an absent result
- A cache failure never reaches the caller — the call falls through to the database

## Configuration

- Profile properties hold only what differs from `application.properties`
- Cache settings live in properties; Redis timeouts stay at framework defaults

## Testing

- Integration classes are suffixed `IntegrationTest`, unit classes `Test`
- One integration class per controller, full stack, no mocks
- Integration classes extend `AppTests`; anything shared belongs there, not duplicated
- Integration tests run against a real Redis and an in-memory database, both reset before every test
- Assert whole bodies with `.content().json(expected, STRICT)`
- JSON payloads live under `mock/` as `{request,response}/<controller>/<payload-type>-<scenario>.json`; single-field and empty bodies come from `JsonUtils`
- A request `<scenario>` names what the payload contains, never the outcome it provokes — an outcome depends on seeded state, so it belongs in the test name
- One seed `.sql` per test class in `mock/sql/`, holding only its `INSERT`s; every `@Sql` runs `/mock/sql/clear-tables.sql` first
- Seeded UUIDs never repeat across files; `user_id` is the same everywhere
- Unit tests use Mockito; `verify(...)` only to assert a collaborator was or was not called

## Docker & Docs

- The image is built from the pre-compiled JAR — run `./mvnw clean package` before `docker compose up`
- `README.md` (English only) and `CLAUDE.md` change in the same set as any change to architecture, conventions or dependencies
- `CLAUDE.md` is at most 200 lines
- A rule written here is never restated in `CLAUDE.md` or `README.md` — they point to this file instead

## Style

- Objects from factory methods or builders rather than direct `new`; factories named `of`
- Method names objective, short, unabbreviated, under ~38 characters
- `validate*` only when the method has a conditional that rejects; otherwise name it after what it does
- A method whose return value no caller reads is `void`
- Test methods `should<Outcome>When<Condition>`, `@DisplayName` reading `Deve retornar <status> [efeito] quando <condição>`
- `var` when the type is clear from the right-hand side
- Static-import constants and enum values, unless two enums share a value's simple name; ordinary static calls stay qualified
- Guard clauses over nested conditionals; method references over lambdas
- No comments or Javadoc
- A class that logs declares `LOG_PREFIX` with its own name, prepended to the message

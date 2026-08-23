# Code Conventions

This file is only edited with the user's prior authorization — ask first before adding,
changing or removing any rule, and state what the code does today that motivates it.

## Architecture

- Controllers mapped under `/v1/...`
- DTOs are immutable `record`s
- Entity ↔ DTO conversion only through MapStruct
- Repositories are Spring Data interfaces — never an implementation class

## Exceptions

- Business errors throw `ServiceException`, never a subclass
- Its text is a constant from `constants/Messages`, without trailing punctuation, deleted with its last reference
- `ProblemDetail` carries `title`, `detail`, `status`, `instance`, plus `errors` for validation failures — never a machine-readable `code`
- `title` states the failure kind, not the specific error

## Monetary Values

- `BigDecimal` with scale exactly 2 — never `double`/`float`
- More than 2 decimals is a validation error at the DTO boundary, never rounded
- Request amounts are `@Positive`; the sign comes from the operation type

## Caching

- Cache annotations live only on the repository
- `cacheNames` is kebab-case
- Never cache an absent result
- A cache failure never reaches the caller — the call falls through to the database

## Configuration

- Profile properties hold only what differs from `application.properties`
- Cache settings live in properties; Redis timeouts stay at framework defaults

## Logging

- No sensitive data in logs — never log HTTP payloads, headers, credentials or personal data; the `correlationId` in the MDC is what ties a line to its request
- A class that logs should declare `LOG_PREFIX` with its own name, prepended to the message — a recommendation, not an absolute: skip it where the message already identifies its own origin

## Testing

- Integration classes are suffixed `IntegrationTest`, unit classes `Test`
- One integration class per controller — full stack, no mocks, extending `AppTests`; anything shared belongs there
- Integration tests run against a real Redis and an in-memory database, both reset before every test
- Test methods `should<Outcome>When<Condition>`, `@DisplayName` reading `Deve retornar <status> [efeito] quando <condição>`
- Assert whole bodies with `.content().json(expected, STRICT)`
- JSON payloads live under `mock/` as `{request,response}/<controller>/<payload-type>-<scenario>.json`; single-field and empty bodies come from `JsonUtils`
- A request `<scenario>` names what the payload contains; the outcome belongs in the test name
- One seed `.sql` per test class in `mock/sql/`, holding only its `INSERT`s; every `@Sql` runs `/mock/sql/clear-tables.sql` first
- Seeded UUIDs never repeat across files; `user_id` is the same everywhere
- Unit tests use Mockito; `verify(...)` only to assert a collaborator was or was not called

## Docker

- The image is built from the pre-compiled JAR — run `./mvnw clean package` before `docker compose up`

## Docs

- `README.md` (English only) and `CLAUDE.md` change in the same set as any change to what a consumer of the API or someone running the project has to know — build, test and CI plumbing does not qualify
- `README.md` is at most 250 lines, `CLAUDE.md` at most 200; `CLAUDE.md` never restates a rule from this file — it points here instead
- Editing either file replaces text rather than appending to it; why a change was made belongs in the commit message, never in the doc

## Style

- Objects from factory methods or builders rather than direct `new`; factories named `of`
- Up to three arguments go through the `of` factory; more than three go through a builder
- `validate*` is optional, and reserved for a method whose only job is to reject; a method that does
  real work may reject as part of it and still be named after what it does
- Static-import constants and enum values, unless two enums share a value's simple name; ordinary static calls stay qualified
- No comments or Javadoc

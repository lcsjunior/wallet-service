# CLAUDE.md

Guidance for Claude Code (claude.ai/code) working in this repository.

Read these instead of asking again — they are not repeated here:

- `README.md` — what the service does, stack, API, errors, setup, CI
- `.claude/rules/code-conventions.md` — how code is written here; read before editing

## Commands

```bash
./mvnw clean package      # build; spotless:check fails the build on unformatted code
./mvnw spotless:apply     # format before committing
./mvnw spring-boot:run    # run
./mvnw test               # tests; requires a running Docker daemon
./mvnw test -Dtest=WalletControllerIntegrationTest
docker compose up --build # app + Redis; run clean package first
```

Running without Docker needs a Redis on `localhost:6379` (`REDIS_HOST`/`REDIS_PORT` to
point elsewhere), or `CACHE_TYPE=none` to skip it.

Profiles: none is production; `test` is applied by the suite; `dev` adds plain-text logs
and the H2 console at `/h2-console` (`jdbc:h2:file:./data/wallet-service`, user `sa`, no
password).

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Invariants

Cheap to break, expensive to discover:

- `WalletTransaction` rows are append-only. Never mutate or delete one to correct a balance — that table is the audit trail.
- A user may own several wallets. Operations address a wallet by `walletId`; `userId` never identifies one.

## Temporary Rules

- Do not create unit test classes (suffix `Test`). Author and maintain only `IntegrationTest` classes. This supersedes the unit-testing rule in `code-conventions.md` until explicitly lifted.

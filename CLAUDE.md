# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=WalletServiceApplicationTests

# Build and run with Docker
docker compose up --build
```

## Architecture

Spring Boot 3.5.16 REST service, Java 21.

Database: H2 relational (embedded). Schema must support full audit traceability.

Idempotency: mutation endpoints (deposit, withdraw, transfer) use a `correlationId` header so retries are safe.

> Code conventions (package naming, layering, DTOs, testing, Docker build) are defined in `.claude/rules/code-conventions.md`.

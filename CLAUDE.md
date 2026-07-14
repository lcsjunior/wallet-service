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

Spring Boot 3.5.16 REST service, Java 21, packaged as `com.example.wallet_service` (note underscore — the artifact name `wallet-service` is invalid as a Java package name).

The service is in its early stages: only the `@SpringBootApplication` entry point exists so far. New code should be placed under `src/main/java/com/example/wallet_service/` following standard Spring layering (controller → service → repository).

The Docker image is built from a pre-compiled JAR (`target/wallet-service-0.0.1-SNAPSHOT.jar`) using `eclipse-temurin:21-jre-alpine`. Run `./mvnw clean package` before `docker compose up`.

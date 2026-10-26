# Spring Boot Boxing Academy API

A Java backend project for managing a boxing academy's members, training schedule, entry-limited passes, bookings and payment records.

**Status: application scaffold. The Maven project compiles and packages; domain features, SQL migrations and authentication workflows are not implemented yet.**

## Planned functionality

- Member, trainer and owner accounts with role-based access.
- Training groups, rooms and individually scheduled classes.
- Monthly passes with a fixed number of entries.
- Booking and cancellation with transactional seat and entry allocation.
- Attendance tracking and owner-recorded cash or bank-transfer payments.

The first version serves one club. Payment records do not execute money transfers. Online payment gateways, refunds, multi-club SaaS and a frontend are outside V1.

## Stack

Java 25, Spring Boot 4.1.2, Spring MVC, Spring Data JPA, Spring Security, Bean Validation, Actuator and Flyway. The included Maven Wrapper uses Maven 3.10.0. The generated test configuration uses PostgreSQL 18.6 through Testcontainers.

Spring Boot manages library versions. Docker Compose, custom security configuration, OpenAPI and GitHub Actions remain planned. See [architecture and technology](docs/architecture.md) and [actual verification](docs/development/progress.md).

## Documentation

- [Documentation index](docs/README.md) - reading guide and current specifications.
- [Scope and completion criteria](docs/scope.md).
- [Architecture](docs/architecture.md), [database](docs/database.md) and [API contract](docs/api.md).
- [Implementation progress](docs/development/progress.md) and [delivery plan](docs/development/delivery-plan.md).

## Development

This directory is the single-module Maven project root; there is no nested application directory.

- Repository directory: `spring-boot-boxing-academy-api`.
- Maven coordinates: `dev.bartmilo:boxing-academy-api`.
- Java base package: `dev.bartmilo.boxingacademy`.

With JDK 25 installed, compile main/test sources and package the application without running tests:

```sh
./mvnw -B -DskipTests verify
```

The first run downloads Maven and dependencies. Tests require a running Docker engine and pull the PostgreSQL image:

```sh
./mvnw test
```

The generated smoke test starts the application context against a disposable PostgreSQL container. It has not yet been executed locally because Docker was not running when the scaffold was created.

With Docker running, the generated development launcher can also supply a disposable database:

```sh
./mvnw spring-boot:test-run
```

This launch command is provided for the next local verification and has not yet been verified. Container data is temporary. A regular `spring-boot:run` needs database configuration, such as `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. Persistent local PostgreSQL via Compose is the next setup step.

Spring Security currently uses framework defaults, including a generated development password. Club accounts, session endpoints and business endpoints do not exist yet. Only the Actuator health endpoint is configured for management exposure.

Flyway owns the schema. Future SQL migrations belong in `src/main/resources/db/migration`, for example `V1__create_accounts.sql`. Hibernate uses `ddl-auto=validate`, Open Session in View is disabled, and separate `schema.sql`/`data.sql` initialization is disabled. No business tables or placeholder migrations have been generated.

Implementation agents should start with [AGENTS.md](AGENTS.md). Keep planned behavior separate from implemented and tested capabilities when updating this README.

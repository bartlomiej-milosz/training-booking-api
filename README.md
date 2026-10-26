# Spring Boot Boxing Academy API

A Java backend project for managing a boxing academy's members, training schedule, entry-limited passes, bookings and payment records.

**Status: application scaffold with a verified local database connection. The local Spring Boot profile starts against Compose PostgreSQL and its health endpoint reports UP. Domain features, SQL migrations and authentication workflows are not implemented yet.**

## Planned functionality

- Member, trainer and owner accounts with role-based access.
- Training groups, rooms and individually scheduled classes.
- Monthly passes with a fixed number of entries.
- Booking and cancellation with transactional seat and entry allocation.
- Attendance tracking and owner-recorded cash or bank-transfer payments.

The first version serves one club. Payment records do not execute money transfers. Online payment gateways, refunds, multi-club SaaS and a frontend are outside V1.

## Stack

Java 25, Spring Boot 4.1.2, Spring MVC, Spring Data JPA, Spring Security, Bean Validation, Actuator and Flyway. The included Maven Wrapper uses Maven 3.10.0. The generated test configuration uses PostgreSQL 18.6 through Testcontainers.

Spring Boot manages library versions. Docker Compose runs local PostgreSQL; custom security configuration, OpenAPI and GitHub Actions remain planned. See [architecture and technology](docs/architecture.md) and [actual verification](docs/development/progress.md).

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

### Local PostgreSQL

Start Docker Desktop. On a fresh checkout, copy `.env.example` to `.env` and set `POSTGRES_PASSWORD` before the first database start. Keep an existing `.env`; it is ignored by Git. A generated local password was created for the initial development setup.

```sh
docker compose up -d --wait postgres
docker compose ps
```

The database is `boxing_academy`, the default user is `boxing_academy`, and the host address is `127.0.0.1:5432`. `.env` can override the name, user and host port. The password remains local. These initialization credentials configure a new database volume; editing `.env` does not change the password of an existing database user.

Compose runs PostgreSQL 18.6 with a readiness check and a named volume mounted at `/var/lib/postgresql`, as required by the PostgreSQL 18 image. The published port is bound to loopback. `docker compose down` removes the container/network while preserving the named volume.

To open SQL using the default database/user inside the running container:

```sh
docker compose exec postgres psql -U boxing_academy -d boxing_academy
```

### Run the application against local PostgreSQL

From the repository root, with PostgreSQL healthy and the existing `.env` configured:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

In another terminal:

```sh
curl --fail http://127.0.0.1:8080/actuator/health
```

The response should report `status: UP`. Stop the application with Ctrl+C; PostgreSQL remains running until you stop it through Compose.

The `local` profile loads `application-local.properties`. Compose reads `.env` automatically; Spring Boot reads it here only because this profile explicitly imports it with `spring.config.import=optional:file:.env[.properties]`. The extension hint treats it as a Java properties file. Keep shared `.env` entries as plain, unquoted `KEY=VALUE` lines; shell commands, shell expansion and dotenv quoting are not supported by this import.

The JDBC URL uses the published host port from `POSTGRES_PORT` (default `5432`), the database name from `POSTGRES_DB`, and credentials from the local configuration. The HTTP server binds to `127.0.0.1:8080` in this profile. This host port is independent of the PostgreSQL port. No password is stored in source files.

Verified locally: HTTP 200 with health `UP`, a real Hikari/JDBC connection in application logs, and PostgreSQL sessions reported under `PostgreSQL JDBC Driver`. Flyway created its history table; no versioned migration or business table exists yet.

### Build and tests

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

This launch command is provided for the next local verification and has not yet been verified. Container data is temporary. A regular `spring-boot:run` needs database configuration, such as `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. The `local` profile supplies the Compose database configuration as described above; an unprofiled launch can use external `SPRING_DATASOURCE_*` settings.

Spring Security currently uses framework defaults, including a generated development password. Club accounts, session endpoints and business endpoints do not exist yet. Only the Actuator health endpoint is configured for management exposure.

Flyway owns the schema. Future SQL migrations belong in `src/main/resources/db/migration`, for example `V1__create_accounts.sql`. Hibernate uses `ddl-auto=validate`, Open Session in View is disabled, and separate `schema.sql`/`data.sql` initialization is disabled. No business tables or placeholder migrations have been generated.

Implementation agents should start with [AGENTS.md](AGENTS.md). Keep planned behavior separate from implemented and tested capabilities when updating this README.

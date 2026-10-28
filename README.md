# Training Booking API

A Java backend project for managing a training club's members, training schedule, entry-limited passes, bookings and payment records.

**Status: room catalog implemented and tested. Spring Boot maps the Flyway room table through JPA and exposes room reads, creation and limited updates. Real club accounts, session authentication and the remaining domain workflows are not implemented yet.**

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

- Repository directory: `training-booking-api`.
- Maven coordinates: `dev.bartmilo:training-booking-api`.
- Java base package: `dev.bartmilo.trainingbooking`.

### Local PostgreSQL

Start Docker Desktop. On a fresh checkout, copy `.env.example` to `.env` and set `POSTGRES_PASSWORD` before the first database start. Keep an existing `.env`; it is ignored by Git. A generated local password was created for the initial development setup.

```sh
docker compose up -d --wait postgres
docker compose ps
```

The database is `training_booking`, the default user is `training_booking`, and the host address is `127.0.0.1:5432`. `.env` can override the name, user and host port. The password remains local. These initialization credentials configure a new database volume; editing `.env` does not change the password of an existing database user.

Compose runs PostgreSQL 18.6 with a readiness check and a named volume mounted at `/var/lib/postgresql`, as required by the PostgreSQL 18 image. The published port is bound to loopback. `docker compose down` removes the container/network while preserving the named volume.

To open SQL using the default database/user inside the running container:

```sh
docker compose exec postgres psql -U training_booking -d training_booking
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

Verified locally: HTTP 200 with health `UP`, a real Hikari/JDBC connection in application logs, and PostgreSQL sessions reported under `PostgreSQL JDBC Driver`. Flyway validated the already-applied V1 migration and the existing room table. The rename preserved both room data and migration history.

### Build and tests

With JDK 25 installed, compile main/test sources and package the application without running tests:

```sh
./mvnw -B -DskipTests verify
```

The first run downloads Maven and dependencies. Tests require a running Docker engine and pull the PostgreSQL image:

```sh
./mvnw test
```

The smoke test starts the application context against a disposable PostgreSQL 18.6 container and applies Flyway migrations. It passed locally after the project rename (1 test, no failures or errors).

With Docker running, the generated development launcher can also supply a disposable database:

```sh
./mvnw spring-boot:test-run
```

This launch command is provided for the next local verification and has not yet been verified. Container data is temporary. A regular `spring-boot:run` needs database configuration, such as `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. The `local` profile supplies the Compose database configuration as described above; an unprofiled launch can use external `SPRING_DATASOURCE_*` settings.

Spring Security currently uses framework authentication defaults, including a generated development password. Method authorization protects the room API; writes require OWNER. Real club accounts and session endpoints do not exist yet. Only the Actuator health endpoint is configured for management exposure.

Flyway owns the schema. Future SQL migrations belong in `src/main/resources/db/migration`, for example `V1__create_accounts.sql`. Hibernate uses `ddl-auto=validate`, Open Session in View is disabled, and separate `schema.sql`/`data.sql` initialization is disabled. The first versioned migration creates room with a UUID primary key; other business tables are not implemented yet.

## Implemented room API

| Method and path | Behavior |
|---|---|
| GET `/api/v1/rooms` | Authenticated; active rooms, paginated with page/size (default 0/20, maximum size 100), ordered by name then ID. OWNER can set includeInactive=true. |
| GET `/api/v1/rooms/{id}` | Authenticated; inactive rooms are visible only to OWNER. |
| POST `/api/v1/rooms` | OWNER and CSRF; name/capacity and optional active flag, default true. Returns 201, UUID DTO and Location. |
| PATCH `/api/v1/rooms/{id}` | OWNER and CSRF; optional name/active only. Capacity is immutable. |

The API trims names, rejects invalid/unknown fields and fractional capacity, reports a duplicate name as 409, and returns ProblemDetail for room validation and business errors. Entities are never serialized directly. UUIDs for JPA inserts are generated by Hibernate; the PostgreSQL default remains available for direct SQL inserts.

After restarting the application with the local profile, try a read:

```sh
curl --user user --header 'Accept: application/json' http://127.0.0.1:8080/api/v1/rooms
```

Curl prompts for Spring Security's generated development password from the application startup log. This is not POSTGRES_PASSWORD. The default user can read rooms but has no OWNER role. Write routes are exercised by integration tests with simulated OWNER identities until real club identity/bootstrap is implemented. These role tests do not prove the planned session login flow; filter-level security responses still use framework defaults.

`./mvnw -B verify` passes 30 tests, including PostgreSQL constraints, UUID mapping, room HTTP behavior, role checks and CSRF rejection. All test data is in disposable Testcontainers databases. The next implementation slice is database-backed accounts, owner bootstrap and the session/CSRF flow needed to use owner writes interactively.

Implementation agents should start with [AGENTS.md](AGENTS.md). Keep planned behavior separate from implemented and tested capabilities when updating this README.

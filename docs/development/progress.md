# Implementation status and next handoff

Last updated: 2026-10-27.

## Current evidence

- Documentation exists for Training Booking API identity, V1 scope, architecture/stack, domain rules, tables, API, tests and delivery.
- The original database proposal and its diagrams are preserved together in docs/reference/.
- V1 Mermaid and editable draw.io diagrams describe 12 business tables; refund/provider-specific data is excluded.
- The existing Git repository and documentation were preserved; the working tree was clean before scaffolding.
- A single-module Maven project was generated with Spring Initializr and merged into the repository root without a nested application directory.
- Main and test sources compile, and Spring Boot packaging succeeds. The regular application now starts with profile `local`, connects to Compose PostgreSQL and reports health UP. The existing context test and new room integration tests pass against PostgreSQL Testcontainers: 30 tests, 0 failures/errors.
- Flyway and its PostgreSQL module are included. Hibernate validates the schema; separate basic SQL initialization and Open Session in View are disabled.
- Docker Compose now runs local PostgreSQL 18.6 with a named volume, readiness check and a loopback-only port. Credentials are in an ignored local `.env`; `.env.example` contains no password.
- The corrected V1 is applied locally after the user recreated the development database. Room has a JPA mapping and HTTP list/read/create/update routes. Real club authentication, other domain workflows and an application container image are not implemented yet.

## Scaffold versions and identity

- JDK: Java 25; local verification used Temurin 25.0.4.
- Spring Boot: 4.1.2, selected from official Initializr metadata on 2026-10-26. Installed Spring CLI is 4.1.0; CLI and generated application versions are separate choices.
- Maven Wrapper: 3.3.4, downloading Maven 3.10.0.
- Boot-managed versions: Flyway 12.4.0, PostgreSQL JDBC 42.7.14, Testcontainers 2.0.5 and JUnit Jupiter 6.0.3.
- Test PostgreSQL image: `postgres:18.6`; use the same PostgreSQL major when adding Compose.
- Coordinates: `dev.bartmilo:training-booking-api`; application name: `training-booking-api` (user-selected); base package: `dev.bartmilo.trainingbooking`.
- Included dependencies: MVC, JPA, PostgreSQL driver, validation, Flyway, Security, Actuator and Testcontainers. Security currently retains framework defaults; identity is not implemented.

## Scaffold verification

- `java -version`: Temurin 25.0.4; `mvn -version`: Maven 3.10.0 using Java 25.
- `/Users/bartek/.sdkman/candidates/springboot/current/bin/spring --version`: Spring CLI 4.1.0. The CLI was installed but not present on the agent shell's PATH; no shell configuration was changed.
- Spring CLI generated the project in a temporary directory using Boot 4.1.2 and the dependency IDs listed above; generated files were reviewed before merging.
- `./mvnw -B -DskipTests verify`: BUILD SUCCESS in the temporary project, compiling one main source and three test sources and creating the executable JAR. Tests were explicitly skipped.
- `docker version`: client 29.6.1 is installed, but the Docker socket did not exist; the engine was not running.
- `./mvnw test` and `./mvnw spring-boot:test-run`: not run. Database startup, Flyway execution, schema validation and the generated context smoke test still require verification with Docker running.

## Local PostgreSQL step

- Added `compose.yaml` and `.env.example`; generated a local password only in the Git-ignored `.env` with owner-only file permissions. Existing application code and schema contracts were unchanged.
- Started the installed Docker Desktop and ran `docker compose config --quiet`: passed.
- `git check-ignore .env`: confirmed the credential file is ignored.
- `docker compose up -d --wait --wait-timeout 60 postgres`: passed; the container is healthy and remains running for the next step.
- `docker compose port postgres 5432`: `127.0.0.1:5432`.
- Used `docker compose exec -T postgres` to run an authenticated TCP `psql` query with `ON_ERROR_STOP=1`: `SELECT current_database(), current_user, version();` returned database/user `training_booking` and PostgreSQL 18.6.
- `git diff --check`: passed before the documentation update. No application tests were run for this configuration-only step.
- Application-to-database connectivity, migrations, table constraints and persistence across container recreation have not been tested. The named volume is configured; it is not evidence of a completed backup/recovery workflow.

## Docker networking learning diagram

Added the editable `Docker port mapping` page to `docs/diagrams/training-booking-api.drawio`. The existing database diagram and user sketch pages were preserved. The page shows a client on macOS, the published loopback endpoint, Docker forwarding and the PostgreSQL container endpoint. It uses host port 5433 for explanation and explicitly distinguishes the current Compose default of 5432. Docker Desktop VM internals are marked as omitted.

Verified XML structure, unique cell IDs and connector references; exported page 3 using draw.io and inspected the PNG for layout and legibility. No application code or Compose settings changed in this explanation step. The next implementation task remains connecting Spring Boot to the local database.

Added the adjacent `Spring TCP connection` learning page: Java/JPA and JDBC inside the JVM, a macOS-assigned example source port, published host port, Docker forwarding and the PostgreSQL listener. It separates TCP establishment, database authentication, SQL exchange and pooled connection reuse. Existing user page names/order and contents were preserved. The page was exported through draw.io and visually checked; no runtime configuration changed.

## Local Spring Boot connection step

- Preserved the user change to `spring.application.name=training-booking-api` and all unrelated working-tree changes.
- Added `src/main/resources/application-local.properties`: explicit `.env` import with a `.properties` extension hint, PostgreSQL JDBC URL/user/password placeholders and loopback-only HTTP binding. The existing `.env` was checked for simple compatible KEY=VALUE entries without printing its password. No new library was needed.
- `docker compose ps` initially showed no running service. `docker compose up -d --wait --wait-timeout 60 postgres` brought the database up healthy.
- `./mvnw -B spring-boot:run -Dspring-boot.run.profiles=local`: started successfully on `127.0.0.1:8080`. Hikari logged a real PostgreSQL connection; Flyway identified PostgreSQL 18.6 at `jdbc:postgresql://127.0.0.1:5432/training_booking`.
- `curl --fail http://127.0.0.1:8080/actuator/health`: HTTP 200, `status: UP` (also includes liveness/readiness group names).
- An SQL read of `pg_stat_activity` showed ten idle `PostgreSQL JDBC Driver` connections. An SQL read of public tables showed only `flyway_schema_history`. No versioned migrations were found; that warning is expected at this stage.
- `lsof` confirmed the HTTP server was listening on loopback. The application was then stopped with Ctrl+C to leave port 8080 free for the user's own launch. PostgreSQL remains running.
- No automated test suite was run for this configuration-only change. Runtime connectivity is verified; business schema constraints and application behavior do not exist yet.

## UUID identifier decision

The user explicitly selected UUID entity identifiers on 2026-10-26. Updated database column types, corresponding foreign keys, API string examples and 26 type labels in the database diagram. Existing learning pages and user page names/order were preserved. Documented PostgreSQL gen_random_uuid() / UUID v4 as the SQL default for newly generated entity IDs; profile shared keys and foreign keys reuse existing values.

Before this change, a read-only check of the local Flyway history showed zero applied versioned migrations and no room table. The user was actively editing V1__create_room.sql, so the migration draft was left untouched. The next step is to finish that migration with a UUID primary key, then verify it through Flyway and test its constraints on PostgreSQL. No migration or application test was executed for this documentation decision.

## Project rename — 2026-10-27

- User-requested identity: Training Booking API; repository/artifact/application name training-booking-api; Java package dev.bartmilo.trainingbooking; application class TrainingBookingApplication.
- Moved the checkout to /Users/bartek/Documents/dev/portfolio-projects/training-booking-api. Updated source/test filenames, Maven metadata, local configuration, documentation, ignored VS Code references and the draw.io filename. The user's pre-existing diagram changes were preserved. Naming references in earlier progress entries were normalized to the current identity; the verification below was actually rerun after renaming.
- Renamed GitHub repository to https://github.com/bartlomiej-milosz/training-booking-api and updated local origin; gh repo view and git ls-remote origin HEAD both succeeded. No source commit or push was performed as part of the rename; file changes remain available for review.
- Copied the stopped PostgreSQL data to the explicitly named volume training-booking-api-postgres-data. The original volume remains unchanged as a fallback. Renamed the copied database and login role to training_booking, preserved the original password, and updated only the database/user entries in local .env. A transient administrator used for the role rename was removed.
- Compared room row-count/content fingerprints and Flyway version/script/checksum/success history before and after the SQL rename: identical. The already-applied V1 migration file is byte-for-byte unchanged.
- ./mvnw -B clean test: BUILD SUCCESS, 1 test, 0 failures, 0 errors, 0 skipped. The new package started and V1 applied on fresh PostgreSQL 18.6 in Testcontainers.
- ./mvnw -B -DskipTests package: BUILD SUCCESS; produced training-booking-api-0.0.1-SNAPSHOT.jar.
- Started that JAR with --spring.profiles.active=local against the preserved database: Flyway validated V1 and reported no pending migration. GET /actuator/health returned HTTP 200 and status UP. PostgreSQL confirmed current database/user training_booking.
- Stopped the verification application with Ctrl+C; the renamed PostgreSQL service remains running. Current implementation scope remains one club and the same planned domain workflows.

## Room catalog slice — 2026-10-27

- At the start, PostgreSQL confirmed the user-reset V1 checksum 640127432 and the CHECK using btrim(name). The migration file was not changed in this slice.
- Added the schedule feature with Room, UUID JPA mapping, repository, concrete transactional service, validated request/response records and controller. Room edits lock the row; capacity has no update path. JPA inserts use provider-generated UUIDs; SQL fixtures use the database default.
- Implemented authenticated paginated list and single-room reads, OWNER creation and name/active edits, active filtering, inactive visibility, Location, trimming, strict JSON fields and stable validation/not-found/duplicate-name ProblemDetail responses.
- Enabled method authorization while preserving existing authentication and CSRF. The default development user has no OWNER role; real user storage, bootstrap and session login are not implemented. Simulated role tests are not evidence of a completed authentication flow.
- ./mvnw -B verify: BUILD SUCCESS; 30 tests, 0 failures, 0 errors, 0 skipped. Tests run against PostgreSQL 18.6 through Testcontainers and exercise real migrations, JPA, HTTP handling, roles, CSRF, constraints, immutable capacity and pagination. The expected duplicate-key warning is from the conflict test.
- Started the packaged JAR on port 8081, leaving the user's existing 8080 process alone. Anonymous GET /api/v1/rooms returned 401; a temporary in-memory MEMBER verifier received 200 and the paginated shape (0 local rooms). Health returned 200 / UP. This check was read-only against the local database. The verifier instance was stopped and its temporary credential file removed.
- Existing user files, scripts and migration were preserved. No commit or push was performed.

## Room code walkthrough

The user requested understanding the implemented code line by line before further feature work. Added the optional root todo.md with a bounded walkthrough of a single room read, links to actual source/test files and a parking lot for later questions. This is code-driven learning, not a parallel course or a release gate. No application code changed and no tests were rerun for this Markdown-only step.

## Milestones

| Slice | Status | Evidence |
|---|---|---|
| Documentation handoff | Prepared | README, AGENTS and docs. |
| Foundation and identity | Local startup and first migration verified | Renamed application starts; V1 room migration and context test pass. Identity remains planned. |
| Schedule | Room catalog only | JPA + room list/read/create/update and PostgreSQL-backed tests; sessions/groups/trainers remain planned. |
| Passes and manual payments | Not started | Specification only. |
| Bookings and attendance | Not started | Specification only. |
| Portfolio finish | Not started | Depends on working implementation. |

## Next task

First review the Room mapping and one GET /api/v1/rooms/{id} flow using todo.md, answering questions against the code. Resume the next feature slice when the user is ready: database-backed identity, controlled owner bootstrap and the real session/CSRF flow needed for OWNER room writes. No memberships, payments or bookings yet.

## Verification performed on documentation

Mechanical checks passed: all local Markdown links resolve; all 12 V1 tables agree across database.md, Mermaid and draw.io; every draw.io column matches the table specification; node IDs are unique and all edges reference existing nodes. The original model was preserved byte-for-byte as a 23,102-byte reference, and both historical diagrams were moved into docs/reference/ without changing their content. No visual rendering claim is made for the draw.io diagram.

## Repository organization

- Root README introduces the project; docs/README.md indexes the detailed documentation.
- Active specifications remain directly under docs/; current diagrams are in docs/diagrams/.
- Delivery plan and this progress record are in docs/development/.
- Historical documents and diagrams are in docs/reference/, marked as non-authoritative.
- AGENTS.md remains at the repository root for automatic discovery.
- Source files and Maven Wrapper now live at the repository root alongside the existing documentation. No empty feature packages or placeholder migrations were added.

## Update after implementation

For each further implementation step, record the exact commands/results, remaining limitations, dependency changes and one next task. Do not mark a milestone complete based on generated files alone.

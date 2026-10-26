# Implementation status and next handoff

Last updated: 2026-10-26.

## Current evidence

- Documentation exists for Spring Boot Boxing Academy API identity, V1 scope, architecture/stack, domain rules, tables, API, tests and delivery.
- The original database proposal and its diagrams are preserved together in docs/reference/.
- V1 Mermaid and editable draw.io diagrams describe 12 business tables; refund/provider-specific data is excluded.
- The existing Git repository and documentation were preserved; the working tree was clean before scaffolding.
- A single-module Maven project was generated with Spring Initializr and merged into the repository root without a nested application directory.
- Main and test sources compile, and Spring Boot packaging succeeds. No test has run and application startup has not yet been verified.
- Flyway and its PostgreSQL module are included. Hibernate validates the schema; separate basic SQL initialization and Open Session in View are disabled.
- No SQL migration, business table, domain endpoint, custom authentication workflow, Compose file or application image exists yet.

## Scaffold versions and identity

- JDK: Java 25; local verification used Temurin 25.0.4.
- Spring Boot: 4.1.2, selected from official Initializr metadata on 2026-10-26. Installed Spring CLI is 4.1.0; CLI and generated application versions are separate choices.
- Maven Wrapper: 3.3.4, downloading Maven 3.10.0.
- Boot-managed versions: Flyway 12.4.0, PostgreSQL JDBC 42.7.14, Testcontainers 2.0.5 and JUnit Jupiter 6.0.3.
- Test PostgreSQL image: `postgres:18.6`; use the same PostgreSQL major when adding Compose.
- Coordinates: `dev.bartmilo:boxing-academy-api`; application name: `boxing-academy`; base package: `dev.bartmilo.boxingacademy`.
- Included dependencies: MVC, JPA, PostgreSQL driver, validation, Flyway, Security, Actuator and Testcontainers. Security currently retains framework defaults; identity is not implemented.

## Scaffold verification

- `java -version`: Temurin 25.0.4; `mvn -version`: Maven 3.10.0 using Java 25.
- `/Users/bartek/.sdkman/candidates/springboot/current/bin/spring --version`: Spring CLI 4.1.0. The CLI was installed but not present on the agent shell's PATH; no shell configuration was changed.
- Spring CLI generated the project in a temporary directory using Boot 4.1.2 and the dependency IDs listed above; generated files were reviewed before merging.
- `./mvnw -B -DskipTests verify`: BUILD SUCCESS in the temporary project, compiling one main source and three test sources and creating the executable JAR. Tests were explicitly skipped.
- `docker version`: client 29.6.1 is installed, but the Docker socket did not exist; the engine was not running.
- `./mvnw test` and `./mvnw spring-boot:test-run`: not run. Database startup, Flyway execution, schema validation and the generated context smoke test still require verification with Docker running.

## Milestones

| Slice | Status | Evidence |
|---|---|---|
| Documentation handoff | Prepared | README, AGENTS and docs. |
| Foundation and identity | Scaffold only | Maven build and test sources compile; runtime, SQL and identity work remain. |
| Schedule | Not started | Specification only. |
| Passes and manual payments | Not started | Specification only. |
| Bookings and attendance | Not started | Specification only. |
| Portfolio finish | Not started | Depends on working implementation. |

## Next task

Continue the foundation in one short session: start Docker, verify the generated context test, then configure persistent local PostgreSQL with Compose and write the first meaningful Flyway migration for the agreed identity model. Verify fresh migration and Hibernate validation before implementing account/session endpoints. Do not generate all V1 tables upfront.

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

# Spring Boot Boxing Academy API

A Java backend project for managing a boxing academy's members, training schedule, entry-limited passes, bookings and payment records.

**Status: design stage. The implementation has not started; this repository currently contains the specification and delivery plan.**

## Planned functionality

- Member, trainer and owner accounts with role-based access.
- Training groups, rooms and individually scheduled classes.
- Monthly passes with a fixed number of entries.
- Booking and cancellation with transactional seat and entry allocation.
- Attendance tracking and owner-recorded cash or bank-transfer payments.

The first version serves one club. Payment records do not execute money transfers. Online payment gateways, refunds, multi-club SaaS and a frontend are outside V1.

## Planned stack

Java 25, Spring Boot, Spring MVC, Spring Data JPA, Spring Security, PostgreSQL, Flyway, Maven, JUnit, Testcontainers, Docker Compose and GitHub Actions.

See [architecture and technology](docs/architecture.md) for the proposed versions and rationale. Actual dependencies will be pinned when the application is created.

## Documentation

- [Documentation index](docs/README.md) - reading guide and current specifications.
- [Scope and completion criteria](docs/scope.md).
- [Architecture](docs/architecture.md), [database](docs/database.md) and [API contract](docs/api.md).
- [Implementation progress](docs/development/progress.md) and [delivery plan](docs/development/delivery-plan.md).

## Development

This directory is the application repository root. The Maven build, source code, migrations and runtime configuration will be added here during implementation. There are no working build or startup commands yet.

Implementation agents should start with [AGENTS.md](AGENTS.md). Keep planned behavior separate from implemented and tested capabilities when updating this README.

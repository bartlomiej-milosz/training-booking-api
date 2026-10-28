# Agent instructions for Training Booking API

## Purpose and language

Build a small, credible Java backend portfolio project that the user can understand and explain. Communicate with the user in Polish. Write all code, comments, documentation, API messages, commit messages and configuration labels in English.

Read README.md, docs/README.md, docs/scope.md, docs/business-rules.md and docs/development/progress.md before implementation; read the other specifications relevant to your slice. The initial task created documentation only. Do not assume a build, server, schema or tests already exist.

## Scope and authority

- Current user instructions take priority. Within this design, scope.md defines V1 boundaries, business-rules.md defines behavior, database.md defines persisted fields/constraints, api.md defines the external contract, and architecture.md defines technical structure.
- These files should agree. If they conflict, identify and resolve the concrete conflict before implementing the affected behavior; do not quietly choose a historical proposal.
- All files in docs/reference/ are historical context, not current implementation specifications. Online payments, refunds and SaaS features are not implementation requirements.
- The product defaults in decisions.md were selected for this implementation brief on 2026-10-23. They are not claims that the user previously approved every detailed business policy. Routine work may proceed with them; flag a materially different policy before changing it.
- Work on one delivery slice at a time, with a short plan and its acceptance checks. Do not generate every module or all migrations in advance.

## Implementation constraints

Use Java, Spring Boot, Spring MVC, Spring Data JPA, PostgreSQL, Flyway, Maven Wrapper, Spring Security and focused tests. Keep one deployable application and one database. Organize packages by feature. Prefer concrete services and explicit small DTO mappings. Do not introduce generic CRUD frameworks, service interfaces with one implementation, Kafka, Redis, CQRS, event sourcing, microservices or Kubernetes.

Spring Data repository interfaces and framework extension points are legitimate interfaces. Do not replace them to satisfy an overly literal ban on interfaces. No custom IoC implementation in this application.

Transactions and database constraints must protect seat capacity, pass entry allocation, payment idempotency and schedule overlaps. A precheck plus insert is not concurrency control. Use the documented lock order in every mutation path.

Use one injected Clock; never sleep to test deadlines. Do not expose JPA entities or accept member/owner identity from client-controlled fields when the principal determines it. Keep CSRF enabled for cookie authentication.

## Safe development workflow

- Inspect local changes, existing instructions and code before editing. Preserve user work.
- Pin compatible stable dependency versions at scaffolding time; do not copy obsolete version numbers or use SNAPSHOT/preview releases. Document the actual selected versions.
- Use Flyway for schema changes; Hibernate validates. Do not change a shared applied migration.
- Use PostgreSQL Testcontainers for database-specific behavior. Do not substitute H2 for constraints, locking or migrations.
- Run the checks appropriate to the change. Report exact commands, results and any unrun checks honestly.
- When the user authorizes commits, use small logical Conventional Commits, e.g. `feat(booking): reserve entries atomically`. Do not create a repository, push, rewrite history or fabricate timestamps merely because this file mentions commits.
- Do not change GitHub, LinkedIn or the CV as a side effect of implementation.

## Handoff after each slice

Update docs/development/progress.md with implemented behavior, changed contracts/migrations, commands actually run, remaining blockers and one recommended next task. Briefly explain the important mechanism and tradeoff in Polish; include up to three focused questions the user can later use to check understanding. Do not make every slice into a full course.

Once the V1 acceptance checklist passes, recommend finishing the project. Optional enhancements are not blockers to portfolio readiness.

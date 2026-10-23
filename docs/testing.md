# Test strategy and acceptance evidence

Test observable business behavior and failure boundaries. No coverage percentage target, tests for every getter or mocks that simply restate service implementation.

## Unit tests

- Pass validity: normal month, January 31, leap-year February, exclusive end date, session crossing midnight, club-local date differing from UTC.
- Booking and cancellation cutoffs: just before, exactly at and just after; explicit Instant values, no sleeps. Include daylight-saving transitions for club-local interpretation.
- Allowed and rejected membership/booking transitions; repeated commands preserve audit timestamps.
- Snapshot semantics: editing an offer does not alter an existing pass.
- Derived entry counts for every booking status, including no bookings and unresolved past RESERVED rows.

Use a fixed/controllable Clock. Do not mock JPA to claim database concurrency correctness.

## PostgreSQL integration tests

Use Testcontainers with the same PostgreSQL major as Compose. Apply Flyway and validate JPA mappings on the real database; H2 is not equivalent.

- Fresh migration execution, constraints, partial unique booking index, composite pass-owner FK and room/trainer exclusion constraints.
- Failure midway through a use case rolls back all changes: payment + activation, reservation, and session cancellation + released entries.
- Owner manual payment replay: same receipt and validity dates; conflicting key reuse rejected; two distinct keys cannot create two manual receipts for one pass.
- State/status-dependent null constraints reject invalid rows, including NULL cases that naive CHECKs would miss.
- Foreign-key/history preservation and immutable purchase snapshots.
- Repository queries return correct schedule/roster/balances without relying on collection iteration order.

## Required concurrency tests

Run contenders using independent connections and transactions; commit setup first. Use barriers/latches and bounded waits, not Thread.sleep or one enclosing test transaction.

| Race | Required invariant |
|---|---|
| Two members, last seat | Exactly one booking succeeds; seat count never exceeds capacity. |
| Same pass, last entry, two different sessions | Exactly one reservation succeeds; available entries never become negative. |
| Same member/session duplicate | At most one non-CANCELLED booking; loser receives a defined conflict. |
| Booking versus session cancellation | Either booking is rejected, or it is included in cancellation; no RESERVED booking remains on a cancelled session. |
| Member cancellation versus session cancellation | Entry released once; club cancellation returns a previously late-consumed entry. |
| Payment replay and distinct-key duplicate | Exactly one receipt and activation; stable original dates. |
| Payment versus unpaid purchase cancellation | One valid outcome; never CANCELLED with a successful receipt or ACTIVE without it. |
| Concurrent overlapping room/trainer creation | At most one conflicting session commits; adjacent intervals both succeed. |

Assert final persisted state and count of outcomes, not a particular thread winning. Keep tests deterministic and close executors/connections.

## HTTP/security tests

- Real authentication flow including CSRF retrieval, login, session rotation, logout and password change. Missing CSRF on an unsafe request fails.
- 401 vs 403 vs private-resource 404; no redirect to an HTML login page.
- Members cannot access another member's pass/payment/booking or create privileged roles.
- Trainers see only assigned rosters and cannot change attendance on another trainer's session.
- Owners can administer without implicitly owning a MEMBER profile; MEMBER+TRAINER accounts behave correctly.
- Blocked accounts rejected at login and with an existing authenticated session.
- No hash, password, unnecessary contact details or SQL internals in responses/errors.
- Pagination bounds, validation fields, enum values, decimal serialization, timestamp offsets and required idempotency headers.

Use focused controller/security tests where sufficient and a smaller full-application suite for end-to-end workflows. Do not claim mocked security tests prove session behavior.

## Build and demo acceptance

Select a Maven test layout so `./mvnw --batch-mode verify` actually includes integration tests (e.g. Surefire unit tests + Failsafe *IT). CI runs it with Docker available; do not silently skip container tests. Add a simple formatter/checker with one documented command.

In a clean checkout, validate build, migrations, `docker compose up --build`, health/readiness and the documented request walkthrough. Include owner account creation, member purchase, payment, booking, cancellation and trainer attendance. Use relative demo dates or a documented demo-only adjustable Clock; do not make reviewers wait for a future class to end. Never expose a clock-control endpoint in normal runtime.

Record commands, environment, counts and failures in [development/progress.md](development/progress.md). Remote CI remains unverified until it actually runs. The current documentation stage has run no Java, database or container tests.

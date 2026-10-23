# Product scope

## Objective

Spring Boot Boxing Academy API is a backend for one boxing club. A member can view classes, purchase an entry-limited pass, have a payment recorded by reception/the owner, book and cancel a place, and inspect their pass balance and attendance history. Owners manage the club catalog and schedule. Trainers see their own class rosters and record attendance.

The distinction between a finished portfolio backend and a live commercial product is intentional. V1 can support a future UI for the workflows below. Public self-service onboarding, real money processing, operational support and legal processes are not claimed to be complete.

## Roles

| Role | V1 capabilities |
|---|---|
| MEMBER | Read the schedule and offers; create/cancel an unpaid purchase; read own passes; reserve/cancel own places; read own history. |
| TRAINER | Read the schedule; read participant names for assigned sessions; record attendance for assigned sessions after they end. |
| OWNER | Manage member/trainer accounts and profiles, rooms, groups, offers and sessions; read purchases; record manual payments; view rosters; cancel sessions; record attendance. |

Roles are additive. OWNER does not implicitly create a member or trainer profile. Public users cannot assign roles. Multiple owners can exist, but owner creation is a controlled bootstrap/administration operation, not an unrestricted HTTP endpoint.

## Included in V1

1. Owner-created MEMBER/TRAINER accounts, sign-in, sign-out, current-user endpoint and authenticated password change. Owners can add MEMBER/TRAINER roles to an existing account with the corresponding profile. No public signup yet.
2. Skill levels BEGINNER, INTERMEDIATE, ADVANCED. Group levels are recommendations, not booking restrictions. Groups are class templates, not fixed enrolled cohorts.
3. Rooms, trainer profiles, training groups and independently scheduled sessions. One room and lead trainer per session. Prevent concurrent overlaps for the same room or trainer.
4. Initial offers: PLN 220 for 8 entries / one calendar month, PLN 260 for 12 entries / one calendar month. These are configurable demo catalog records, not hard-coded application logic.
5. Purchased passes with immutable commercial snapshots, activation on manual payment and derived expiry/entry balances.
6. Manual CASH/BANK_TRANSFER payment records created by an authenticated owner. Payment recording is idempotent; it does not execute a financial transfer.
7. Capacity- and pass-aware reservations, timely/late member cancellation and club cancellation. Concurrent requests cannot oversell seats or spend the last entry twice.
8. Attendance marking: ATTENDED / NO_SHOW after a class ends. Owner and assigned trainer only.
9. API validation, consistent errors, pagination, OpenAPI documentation and a reproducible demo.
10. PostgreSQL migrations, meaningful tests, CI and Docker Compose for application + database.

## Deliberate V1 limits

- Adults-only demonstration; no birth dates, medical details or guardian workflows.
- One club, one configured timezone (Europe/Warsaw), PLN only, no tenant/club_id added for imagined SaaS use.
- No recurring timetable generator: create dated sessions individually.
- No session rescheduling endpoint: cancel and recreate an upcoming class. Session capacity and room capacity are immutable after creation in V1. New sessions may use updated group defaults.
- No role removal, account-blocking administration or arbitrary attendance corrections through V1 APIs. The schema still supports BLOCKED accounts and authentication must reject them. Those maintenance workflows need explicit rules before adding endpoints.
- No automatic pass selection; the member selects the pass when booking.
- No trainer-specific payroll, private coaching marketplace, competitions or training-performance tracking.
- No booking overlap restriction for a member in V1; the UI may warn later. Room/trainer overlap prevention is mandatory.

## Deferred; never a V1 completion blocker

Online gateways/webhooks, refunds/revocation, email verification, forgotten-password email recovery, public signup, recurring subscriptions, invoices, pass freezing/transfers, waitlists, notifications, multiple clubs, SaaS billing, analytics dashboards, frontend, cloud deployment, Kubernetes, Kafka and Redis.

If the application is later opened to real customers, separately plan identity recovery/verification, abuse controls, backups, monitoring, privacy/legal requirements and payment-provider operations. A local demo is not evidence of those capabilities.

## Definition of complete

- A fresh checkout can start the documented local demo with known prerequisites and no checked-in secrets.
- A member can complete the whole purchase -> owner payment record -> booking -> attendance workflow through documented API calls.
- Cancellation deadlines, pass validity and time boundaries behave as documented.
- Access controls prevent cross-member reads/writes and trainer access to another trainer's roster.
- Migrations work on empty PostgreSQL; ORM schema validation passes.
- Tests demonstrate the seat/entry races, payment replays and scheduling conflicts.
- CI runs the actual build/tests/format check; local and remote results are distinguished.
- OpenAPI and request examples match the implementation; no fake endpoints or placeholders.
- README states scope and limitations, includes run/test/demo steps and a small architecture diagram.
- The author can explain the workflow, transaction boundaries and tradeoffs. Technical readiness can be reported before the separate learning review.

Stop adding portfolio features once these criteria pass.

# Decision record

Prepared 2026-10-23. This is a compact implementation brief, not a claim of deployed behavior.

## Preserved from the earlier design

- One club, not multi-tenant SaaS; owner/trainer/member roles may overlap.
- Open class groups with recommended proficiency, individually dated sessions and one lead trainer/room.
- Entry-limited calendar-month passes, PLN examples 220/8 and 260/12.
- Price/entitlement snapshots, activation after successful payment, explicit pass selection for booking.
- Reservation holds an entry; attendance/no-show/late cancellation consumes it; club cancellation releases it.
- Transactions, real database constraints and retained history rather than counters maintained in parallel.

## Confirmed project name

On 2026-10-27 the user selected **Training Booking API** and repository name `training-booking-api` to describe the system independently of a particular training discipline. The Java package is `dev.bartmilo.trainingbooking`. The project still serves one training club with the same schedule, booking, pass, attendance and manual-payment scope; no multi-tenancy or generic plugin system is introduced. This naming decision is confirmed; the business-policy defaults below remain recommendations.

## Confirmed entity identifier policy

On 2026-10-26 the user selected UUID identifiers as a consistent project convention. Use PostgreSQL `uuid` for entity primary keys and all corresponding foreign keys, `java.util.UUID` in Java and UUID strings in API contracts. New entity rows use `DEFAULT gen_random_uuid()` (UUID v4) for SQL inserts that omit their ID. Profile primary keys shared with app_user and other foreign keys reuse the referenced ID; they do not independently generate it. The existing role-code part of the user_role composite key remains text.

The tradeoff is larger identifiers and longer manual queries in exchange for independently generatable, non-sequential IDs. UUID is a project choice, not a universal requirement or an access-control mechanism. The default generates a value, while PRIMARY KEY enforces uniqueness and non-nullness. The Room JPA mapping uses java.util.UUID and GenerationType.UUID: Hibernate assigns the ID before its INSERT. The database DEFAULT gen_random_uuid() is used when a direct SQL INSERT omits the ID; it does not replace an ID already supplied by JPA.

## Defaults selected for this handoff

These are deliberate recommendations to make V1 implementable. They were not all previously explicit user decisions. They may be revised before affected implementation; keep the specification and tests consistent if changed.

| Decision | Reason / consequence |
|---|---|
| Training Booking API | User-selected descriptive name; repository slug `training-booking-api`. The name describes training reservations without tying the backend to a particular sport. |
| Single-module modular monolith | Demonstrates normal backend work without deployment/distribution overhead. |
| Java 25, Boot 4.1.x, PostgreSQL 18 | Concrete compatible baseline; pin stable patch versions at scaffolding and recheck compatibility. |
| Cookie/session authentication | Fits a future same-origin web UI without custom refresh tokens; requires CSRF and session handling. |
| Owner-created accounts in V1 | Registration is available to club staff; public signup/email recovery is a separate complete workflow, not an unfinished stub. |
| Manual receipts only | Gives the membership lifecycle a real executable flow without claiming to process money. V1 has one receipt per pass; online payments would require a new attempt/reconciliation model. |
| Two-hour free cancellation cutoff | Clear testable default. Store the actual deadline on the session so future changes do not rewrite history. |
| Attendance after session end | One simple rule for both attendance states; avoids modifying occupancy during an active class. |
| No reschedule/capacity edits | Cancel/recreate provides a correct bounded alternative to complicated revalidation of existing bookings/pass coverage. |
| No member overlap restriction | Not needed for capacity/entry correctness; documented so agents do not invent additional rejection rules. |
| No refunds or active-pass revocation | Avoids inconsistent financial/booking corrections until that workflow is explicitly designed. |
| PostgreSQL exclusion constraints now | Overlap correctness under concurrency is part of scheduling, not an optional later polish. |
| No persisted balance counters | The small data volume supports aggregate reads under locks; one clear source of truth. |
| Plain DTO mappings, no service interfaces by default | Fewer layers; framework interfaces remain appropriate. |

## Clarifications to the original broad schema

The historical proposal included ONLINE/PENDING/FAILED payment attempts, provider identifiers, refunds, revoked passes, email verification and webhook event tables. None of those is silently required by V1. The canonical database document and V1 diagrams remove the associated fields/tables/states.

The original model allowed ATTENDED after a session starts and contemplated changes to unbooked sessions. V1 deliberately narrows these to attendance after the end and no rescheduling endpoint. This reduces edge cases while preserving the core club workflow.

## Change discipline

No new decision is needed for a local variable name, a small mapper or a routine test. Update this file when a decision materially changes scope, business policy, authorization, persistence or compatibility. Do not build an ADR framework or require approval for harmless implementation details.

There are no unresolved questions blocking the first implementation slice. Before introducing real users, online payments, multi-club operation or broader schedule edits, define the corresponding product and operational rules instead of assuming V1 already supports them.

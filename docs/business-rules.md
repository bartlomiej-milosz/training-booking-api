# Business rules and transaction contracts

These rules are the V1 implementation defaults. They refine the earlier database proposal; see decisions.md for what was selected now rather than previously confirmed by the user.

## Time and money

- Use one injected Clock and a configured ZoneId, initially Europe/Warsaw.
- API timestamps include an offset; persist Instant/timestamptz. Do not accept ambiguous local date-times. Responses use UTC ISO-8601 timestamps and expose the configured timezone where useful.
- Pass validity is a pair of club-local dates `[valid_from, valid_until)`. On payment, valid_from is today's club-local date; valid_until is `valid_from.plusMonths(validity_months)`. Use calendar-month end clamping, not 30 days. January 31 + one month is February 28 (29 in a leap year).
- A session is covered only if its start's club-local date is within that interval. The booking date alone is insufficient. A session may end after midnight; eligibility uses its start.
- PLN only; positive monetary values stored as numeric(12,2)/BigDecimal. Never float/double. API amounts are decimal strings, e.g. `"220.00"`.
- Plan name, price, currency, entry_limit and validity_months are copied into each purchased membership. Catalog edits cannot change existing contracts.

## Identity and catalog

- Normalize account email by trimming and lowercasing under one documented policy. No role choice by unauthenticated callers.
- Owner-created accounts receive MEMBER, TRAINER or both; create required profiles in the same transaction. Adding either role later also creates its profile atomically. OWNER accounts are bootstrapped outside the public API.
- Account status ACTIVE is required for authenticated access. Only active trainers can be assigned to new sessions. Deactivating a catalog item does not retroactively cancel existing sessions, purchases or bookings.
- Group skill level is advisory. No roster enrollment, group-specific pass price or automatic promotion algorithm.
- Room capacity is immutable after creation in V1. Session capacity must not exceed room capacity. Group defaults are copied into a session and can be overridden at creation within room limits.
- Group, plan and room active flags control new use, not historical visibility. Sessions expose their own concrete capacity/dates.

## Session scheduling

A session has one trainer, room and group. It starts in the future and ends after it starts. Trainer, room and group must be active at creation.

Default booking_closes_at = starts_at. New booking is allowed only while `now < booking_closes_at` and `now < starts_at`.

Default free_cancellation_until = starts_at minus two hours. Timely cancellation is allowed at the exact cutoff (`now <= free_cancellation_until`); later cancellation before starts_at is late. Store the cutoff in the session so future configuration changes do not rewrite its policy.

No rescheduling or capacity editing endpoint in V1. Cancel an upcoming session and create a replacement. Both room and trainer intervals must be non-overlapping for SCHEDULED sessions, using half-open `[starts_at, ends_at)` ranges. A precheck improves messages; database exclusion constraints are the final concurrency guard.

## Pass purchase and payment

`PENDING_PAYMENT -> ACTIVE` after one successful owner-recorded payment.
`PENDING_PAYMENT -> CANCELLED` when the member or owner cancels the unpaid purchase.
No ACTIVE -> CANCELLED/REVOKED transition in V1. Expiry and exhaustion are calculated, not persisted states.

A purchase requires an active plan and MEMBER identity. It snapshots the offer in one transaction. Changing a plan later must not alter payment amount or entitlements. Multiple overlapping passes are allowed.

Owner recording a payment:

1. Authenticate/authorize the owner; require a UUID Idempotency-Key.
2. Lock the target membership. Check for an existing receipt with that key.
3. A replay with the same owner, membership and method returns the existing receipt (200), without extending dates. Reusing a key for a different request returns 409.
4. A first recording requires PENDING_PAYMENT. Server takes amount/currency from the membership, actor from authentication and paid_at from Clock. The client cannot choose a price, owner ID or historic payment date.
5. Persist the SUCCEEDED CASH/BANK_TRANSFER receipt and activate the membership atomically; respond 201. Unique membership and idempotency constraints handle races, including equal keys used for different memberships. Handle uniqueness failures outside the rolled-back transaction before deciding replay/conflict.
6. A new key for an already paid membership returns 409. No actual money transfer occurred in this workflow, so rejecting a duplicate manual receipt is appropriate.

Payment recording and unpaid cancellation both lock the membership. A failed transaction must leave neither a half-activated pass nor a detached payment record. Payments cannot be edited/deleted through V1 APIs; corrections/refunds require a future explicit policy.

## Entry accounting and booking

- RESERVED holds one entry.
- ATTENDED, NO_SHOW and LATE_CANCELLED each consume one entry.
- CANCELLED consumes/holds zero.
- Available entries = entry_limit - count(RESERVED, ATTENDED, NO_SHOW, LATE_CANCELLED) for that pass.
- Upcoming seats available = capacity - count(RESERVED) for that session. A late cancellation releases the seat, but not the pass entry.
- Past unresolved RESERVED bookings continue to hold their entry until attendance is recorded. Time passing must not silently return entries.

A member selects session_id and membership_id. Resolve member_id from the authenticated principal, not JSON. The account must be active, the pass must belong to that member and be ACTIVE with date coverage, and both a seat and an entry must be available. The session must still be SCHEDULED and bookable.

Prevent another non-CANCELLED booking for the same member/session. A new attempt after timely cancellation creates a new row. Late cancellation is final for that member/session in V1, even if another pass is available.

## Cancellation and attendance

| Action | Preconditions | Result |
|---|---|---|
| Member cancels own RESERVED booking | Before starts_at, at/before free cutoff | CANCELLED; return entry and seat. |
| Member cancels own RESERVED booking | After free cutoff, before starts_at | LATE_CANCELLED; return seat only. |
| Same member repeats cancellation | Already CANCELLED/LATE_CANCELLED | Return current state, no new effects or timestamp changes. |
| Owner cancels session | SCHEDULED and now < starts_at; non-empty reason | Session CANCELLED; RESERVED and LATE_CANCELLED bookings become CANCELLED, releasing entries. Preserve rows and record owner/reason. |
| Owner repeats session cancellation | Already CANCELLED | Return current state; do not rewrite audit facts. |
| Trainer/owner records attendance | Assigned trainer or owner; SCHEDULED; now >= ends_at; booking RESERVED | ATTENDED or NO_SHOW with server time and authenticated actor. |
| Same attendance submitted again | Same final attendance state and authorized actor | Return current state without rewriting audit fields. |
| Attempt to change final attendance | Different final state, or cancelled booking/session | 409; corrections are deferred. |

Member cancellation at/after class start returns 409. Attendance is deliberately recorded only after the end for both ATTENDED and NO_SHOW: this keeps V1 behavior uniform and avoids mid-class seat accounting. Automatic no-show jobs are unnecessary for V1.

Class cancellation does not erase earlier timely cancellations. Existing CANCELLED rows and their original audit facts remain unchanged. It does release entries held by LATE_CANCELLED rows because the club ultimately did not deliver the session.

## Locking contract

Use PostgreSQL READ COMMITTED with pessimistic row locks for aggregate allocation. Do not use application-level Java locks as database concurrency protection.

- Booking create/cancel and attendance: lock `training_session` first, then the relevant `membership`, then reread the booking/state and count affected rows. Read current time after acquiring locks before checking deadlines.
- Class cancellation: lock the session first, then affected memberships in ascending ID order, then update bookings and the session in the same transaction. This prevents a partially released cancellation.
- Payment and unpaid purchase cancellation: membership lock only; they must not later request session locks.
- Session creation: database exclusion constraints enforce overlap. Lock the room/trainer/group rows in one documented order during active/capacity validation if catalog edits can race creation. Catalog edits must take the same row lock for that record. Creation need not lock existing session rows.
- Do not add an endpoint that takes membership and then session locks. Every writer affecting counts must obey the same strategy.
- Keep database locks until commit; no network calls while holding them. Count bookings after the locks, not from an earlier cache/DTO.
- Translate duplicate/exclusion conflicts to stable 409 errors. A bounded lock timeout is a transient 503 response, not proof that a class is full; log the cause without database internals in the API.

The single-session lock serializes contenders for a last seat. The pass lock serializes contenders for a last entry across different sessions. Unique keys and composite FKs remain independent final guards.

# API contract for V1

This is a design contract, not an executable OpenAPI document. Implement and document each endpoint with its slice; generate OpenAPI from the actual application before completion. Prefix business routes with `/api/v1`.

## Conventions

- JSON camelCase DTOs; never serialize JPA entities or password hashes.
- Entity IDs use UUID strings in JSON, path parameters and filters; Java uses java.util.UUID. Reuse the same ID in persistence and the API rather than adding a separate public-ID column. IDs are identifiers, not authorization.
- Dates: `YYYY-MM-DD`. Timestamps: ISO-8601 with offset on input, UTC `Z` on output. Amounts: decimal strings plus currency.
- Lists: `{ "items": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }`. Maximum size 100; validate nonnegative page and positive size. Whitelist sort fields and include ID as a stable tie-breaker. Schedule defaults to startsAt,id ascending.
- 201 + Location for creation, 200 for reads/commands/replays, 204 for logout or successful password change. No redirects.
- 400 malformed/invalid input; 401 unauthenticated; 403 wrong role or CSRF failure; 404 absent resource or another member's private resource; 409 domain/uniqueness/overlap conflict. 503 for a transient lock timeout. Do not return 500 for expected business conflicts.
- Response errors use Spring ProblemDetail (`application/problem+json`) with type, title, status, detail, instance and an application `code`. Validation adds `errors` containing field/message pairs. Never expose SQL, stack traces, credential values or rejected passwords.
- No invented timestamps, price totals, balances or payment actors from the client. Derive them server-side.

## Authentication

| Method and path | Input / result |
|---|---|
| GET `/auth/csrf` | Anonymous or signed in; returns token and headerName; Cache-Control: no-store. |
| POST `/auth/login` | Form-encoded email/password, CSRF header and session cookie; 200 on success, 401 generic failure. |
| POST `/auth/logout` | CSRF required; invalidate session, 204. |
| GET `/me` | Current identity, roles and available profiles; 401 without authentication. |
| PUT `/me/password` | JSON currentPassword/newPassword; validate current password, change hash and invalidate sessions; 204. |

These paths also use the `/api/v1` prefix. Use framework authentication filters and handlers rather than a controller that manually stores a user ID in a session. Clients fetch a new CSRF token after login/logout and sign in again after password change.

## Accounts and catalog

| Method and path | Access and intent |
|---|---|
| POST `/users` | OWNER creates account with email, initialPassword, names, optional phone and MEMBER/TRAINER roles. No OWNER assignment. Create corresponding profiles atomically. |
| GET `/users` | OWNER; paginated staff administration view, optional role filter; no hashes. |
| POST `/users/{id}/roles` | OWNER adds MEMBER or TRAINER and its profile; existing role returns current result without duplication. |
| GET `/skill-levels` | Authenticated; reference values. |
| GET `/trainers` | Authenticated; public-in-club trainer DTOs only, no contact details. |
| PATCH `/trainers/{id}` | OWNER; bio/active only. |
| GET `/groups`, `/rooms`, `/membership-plans` | Authenticated; active catalog by default; OWNER may include inactive entries. |
| POST `/groups`, `/rooms`, `/membership-plans` | OWNER creates catalog entry. |
| PATCH `/groups/{id}` | OWNER updates descriptive fields/defaults/active; no changes to existing sessions. |
| PATCH `/rooms/{id}` | OWNER updates name/active only; capacity immutable in V1. |
| PATCH `/membership-plans/{id}` | OWNER updates future offer/active; purchased snapshots unchanged. |

Account creation accepts role-specific initial profile data (member skillLevelId optional; trainer bio optional). Member joinedOn is the club-local server date. Role addition uses the same profile defaults. No arbitrary user-data edits, hard delete, account-status management or role-removal endpoint in V1.

Validate non-empty names, email format/length, bounded field lengths, positive capacities/amounts and known skill-level IDs. Allow only the documented writable fields; reject unsupported role/status values. Use a documented password policy with a reasonable maximum and no silent truncation.

## Schedule

| Method and path | Access and intent |
|---|---|
| GET `/sessions` | Authenticated; paginated date range (from inclusive, to exclusive), optional groupId/trainerId/status. Bound the range to 90 days; default next 14 days. |
| GET `/sessions/{id}` | Authenticated; schedule DTO including current availableSeats. No participant list. |
| POST `/sessions` | OWNER; groupId, trainerId, roomId, startsAt, endsAt, optional capacity and cutoff overrides. Defaults copied server-side. |
| POST `/sessions/{id}/cancellation` | OWNER; reason required; 200 with resulting cancelled session, including replay. |
| GET `/sessions/{id}/bookings` | OWNER or assigned TRAINER; paginated roster, optionally filtered by booking status. |

A schedule response includes IDs/display names of group, trainer and room; startsAt/endsAt, cutoffs, capacity, availableSeats and status. Availability is a snapshot: only a successful booking reserves a seat.

## Passes and payments

| Method and path | Access and intent |
|---|---|
| POST `/memberships` | MEMBER; `{ "planId": "3583843b-1d5a-4318-993b-fbacb012f105" }`; creates own PENDING_PAYMENT pass. |
| GET `/me/memberships` | MEMBER; own passes, including snapshots, validity and held/used/available entries. |
| GET `/memberships/{id}` | Owning MEMBER or OWNER; private-resource authorization. |
| GET `/memberships` | OWNER; paginated, filter memberId/status. |
| POST `/memberships/{id}/cancellation` | Owning MEMBER or OWNER; only unpaid pass; already CANCELLED returns current state. |
| POST `/memberships/{id}/payments` | OWNER; `{ "method": "CASH" }` or BANK_TRANSFER; UUID Idempotency-Key header required. |
| GET `/memberships/{id}/payments` | Owning MEMBER or OWNER; payment receipts, no secrets. |

New payment record returns 201 + Location `/api/v1/payments/{id}`; add GET `/payments/{id}` for OWNER or the owning MEMBER. Replay returns 200 and the same receipt ID. Different payload/actor under the same key yields 409 IDEMPOTENCY_KEY_REUSED. Amount and currency come from the pass, not the JSON body.

Purchase creation is not idempotent in V1: a retried POST may create another unpaid pass. Document this behavior; the client can list and cancel duplicates. Only payment recording promises an Idempotency-Key contract.

## Bookings and attendance

| Method and path | Access and intent |
|---|---|
| POST `/bookings` | MEMBER; sessionId and membershipId; server derives memberId. |
| GET `/me/bookings` | MEMBER; paginated own history with date/status filters. |
| GET `/bookings/{id}` | Owning MEMBER, OWNER or assigned TRAINER; role-appropriate DTO. |
| POST `/bookings/{id}/cancellation` | Owning MEMBER; timely/late behavior determined server-side. |
| PUT `/bookings/{id}/attendance` | OWNER or assigned TRAINER; `{ "status": "ATTENDED" }` or NO_SHOW after session end. |

Example booking body:

```json
{ "sessionId": "350bf44c-e530-49e0-b8af-03a0be197b52", "membershipId": "ba16a8c1-a30e-4504-a4b5-09e7f078fd70" }
```

Example capacity conflict (illustrative, not evidence of an implemented endpoint):

```json
{
  "type": "about:blank",
  "title": "Booking conflict",
  "status": 409,
  "detail": "The session has no available places.",
  "instance": "/api/v1/bookings",
  "code": "SESSION_FULL"
}
```

Other stable codes should distinguish PASS_NOT_ACTIVE, PASS_OUTSIDE_VALIDITY, NO_ENTRIES_AVAILABLE, BOOKING_ALREADY_EXISTS, BOOKING_CLOSED, SESSION_CANCELLED, SCHEDULE_CONFLICT and INVALID_STATE_TRANSITION. Do not leak another member's pass ownership through a special error: return 404 for inaccessible private passes.

## Frontend handoff

Document the CSRF/login/cookie sequence, authentication expiry, UTC timestamps and club timezone, pagination, validation error shape and mutation conflicts. Include an executable request walkthrough using cookies and CSRF, with OWNER, MEMBER and TRAINER credentials from the opt-in local demo. Swagger alone is not a complete walkthrough.

OpenAPI must document authorization and example responses; its security scheme must match cookie authentication. No frontend implementation is required to prove this contract.

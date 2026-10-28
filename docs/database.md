# V1 database model

Status: target V1 specification. Only room is implemented by an applied SQL migration; the remaining tables are planned. This document defines the V1 columns; [business rules](business-rules.md) define their behavior.

The [original broader proposal](reference/original-data-model.md) is preserved for context. V1 intentionally omits refunds, online-provider fields, payment-event inboxes, verification/reset tokens and revoked passes. Do not implement those from the historical diagram.

There are 12 business tables. Authentication uses framework-managed HTTP sessions in memory for a single application instance; no custom token table is needed in V1.

## Reading the model

`PK` = primary key, `FK` = foreign key. `NULL` is allowed only where stated. Composite keys combine columns. Entity identifiers and their referencing columns use PostgreSQL `uuid`, mapped to `java.util.UUID`. Newly created entity IDs have `DEFAULT gen_random_uuid()` (UUID v4) for database-side generation when omitted from an INSERT. Shared profile IDs and foreign keys reuse an existing identifier and must not generate a new one. Composite keys retain their role-code components. IDs may be exposed as JSON strings; every request still requires authorization. IDs are not access tokens.

A table is not automatically a Java class. This model describes persisted facts and constraints; it does not prescribe services, DTOs, inheritance or interfaces.

## `app_user`

Login identity and contact details. One account may have several roles.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `id` | `uuid` | PK, DEFAULT gen_random_uuid() | Internal identifier. |
| `email` | `varchar(254)` | NOT NULL, UNIQUE after normalization | Store a trimmed, lowercase email using an explicitly documented policy. |
| `password_hash` | `text` | NOT NULL | Password hash produced by the authentication framework, never a plaintext password. |
| `first_name` | `varchar(100)` | NOT NULL | Given name. |
| `last_name` | `varchar(100)` | NOT NULL | Family name. |
| `phone` | `varchar(30)` | NULL | Optional contact number; not a numeric quantity. |
| `status` | `varchar(20)` | NOT NULL, CHECK | ACTIVE or BLOCKED. V1 account creation sets ACTIVE; blocking administration is deferred. |
| `created_at` | `timestamptz` | NOT NULL | Account creation time. |

## `user_role`

Association table. OWNER, TRAINER and MEMBER are fixed role codes; the owner performs administrator functions.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `user_id` | `uuid` | PK part, FK -> app_user.id | Account holding the role. |
| `role` | `varchar(20)` | PK part, CHECK | OWNER, TRAINER or MEMBER. Composite primary key: (user_id, role). |

## `skill_level`

Small reference table shared by member profiles and training groups.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `id` | `uuid` | PK, DEFAULT gen_random_uuid() | Identifier. |
| `code` | `varchar(30)` | NOT NULL, UNIQUE | BEGINNER, INTERMEDIATE or ADVANCED. |
| `name` | `varchar(100)` | NOT NULL | Display name. |

## `member_profile`

An account participating in club activities. This is not a purchased membership.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `user_id` | `uuid` | PK, FK -> app_user.id | One member profile per account. |
| `skill_level_id` | `uuid` | NULL, FK -> skill_level.id | Current assessed level; NULL means not assessed. |
| `joined_on` | `date` | NOT NULL | Date of joining the club. |

## `trainer_profile`

Trainer-specific details. The profile allows sessions to reference trainers rather than arbitrary users.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `user_id` | `uuid` | PK, FK -> app_user.id | One trainer profile per account. |
| `bio` | `text` | NULL | Public trainer description. |
| `active` | `boolean` | NOT NULL | Whether new sessions may be assigned to this trainer. |

## `training_group`

A reusable type of group class, such as Beginner Training. It is not a fixed roster and not a dated session.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `id` | `uuid` | PK, DEFAULT gen_random_uuid() | Identifier. |
| `name` | `varchar(120)` | NOT NULL, UNIQUE | Group name. |
| `description` | `text` | NULL | Description. |
| `recommended_level_id` | `uuid` | NULL, FK -> skill_level.id | Recommended level; NULL means open level. Informational in version 1, not an automatic eligibility gate. |
| `default_capacity` | `integer` | NOT NULL, CHECK > 0 | Default copied when creating a session. |
| `default_duration_minutes` | `integer` | NOT NULL, CHECK > 0 | Default duration. |
| `active` | `boolean` | NOT NULL | Whether the group can be used for new sessions. |

## `room`

A physical training space within the single club.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `id` | `uuid` | PK, DEFAULT gen_random_uuid() | Identifier. |
| `name` | `varchar(100)` | NOT NULL, UNIQUE | Room name. |
| `capacity` | `integer` | NOT NULL, CHECK > 0 | Maximum supported participant count. |
| `active` | `boolean` | NOT NULL | Whether the room can be scheduled. |

## `training_session`

One dated class with one room and one lead trainer. Dates and capacity belong to this session, not just its template.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `id` | `uuid` | PK, DEFAULT gen_random_uuid() | Identifier. |
| `group_id` | `uuid` | NOT NULL, FK -> training_group.id | Class group/template. |
| `trainer_id` | `uuid` | NOT NULL, FK -> trainer_profile.user_id | Assigned trainer. |
| `room_id` | `uuid` | NOT NULL, FK -> room.id | Assigned room. |
| `starts_at` | `timestamptz` | NOT NULL | Start instant. |
| `ends_at` | `timestamptz` | NOT NULL, CHECK > starts_at | End instant. |
| `booking_closes_at` | `timestamptz` | NOT NULL, CHECK <= starts_at | Latest instant for a new reservation. |
| `free_cancellation_until` | `timestamptz` | NOT NULL, CHECK <= starts_at | Latest instant for cancellation without consuming an entry. |
| `capacity` | `integer` | NOT NULL, CHECK > 0 | Concrete session limit; changing group defaults does not modify it. |
| `status` | `varchar(20)` | NOT NULL, CHECK | SCHEDULED or CANCELLED. A past session is inferred from its end time. |
| `cancelled_at` | `timestamptz` | NULL | Required exactly when status is CANCELLED. |
| `cancellation_reason` | `text` | NULL | Reason visible to participants. |
| `created_at` | `timestamptz` | NOT NULL | Creation time. |

## `membership_plan`

The current commercial offer. Purchased memberships preserve their own contract values.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `id` | `uuid` | PK, DEFAULT gen_random_uuid() | Identifier. |
| `name` | `varchar(120)` | NOT NULL | For example: Monthly 8 or Monthly 12. |
| `price_amount` | `numeric(12,2)` | NOT NULL, CHECK > 0 | Price, e.g. 220.00 or 260.00. |
| `currency` | `varchar(3)` | NOT NULL, CHECK = PLN | Version 1 accepts PLN only. |
| `entry_limit` | `integer` | NOT NULL, CHECK > 0 | For example 8 or 12. |
| `validity_months` | `integer` | NOT NULL, CHECK > 0 | One calendar month in the initial offer. |
| `active` | `boolean` | NOT NULL | Whether new purchases are allowed. |

## `membership`

One purchased pass owned by one member. Price and entitlement snapshots are intentional historical facts.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `id` | `uuid` | PK, DEFAULT gen_random_uuid() | Identifier. |
| `member_id` | `uuid` | NOT NULL, FK -> member_profile.user_id | Pass holder. |
| `plan_id` | `uuid` | NOT NULL, FK -> membership_plan.id | Source offer. |
| `plan_name` | `varchar(120)` | NOT NULL | Name at purchase. |
| `price_amount` | `numeric(12,2)` | NOT NULL, CHECK > 0 | Agreed price at purchase. |
| `currency` | `varchar(3)` | NOT NULL, CHECK = PLN | Agreed currency. |
| `entry_limit` | `integer` | NOT NULL, CHECK > 0 | Agreed number of entries. |
| `validity_months` | `integer` | NOT NULL, CHECK > 0 | Agreed duration. |
| `status` | `varchar(20)` | NOT NULL, CHECK | PENDING_PAYMENT, ACTIVE or CANCELLED. Expiry and exhaustion are derived; revocation is deferred. |
| `valid_from` | `date` | NULL | Set on first successful payment, using the club timezone. |
| `valid_until` | `date` | NULL | Exclusive end date; must be later than valid_from. Both dates are either NULL or populated. |
| `activated_at` | `timestamptz` | NULL | Set once on activation; never changed by a replayed payment request. |
| `created_at` | `timestamptz` | NOT NULL | Purchase creation time. |

## `payment`

A successful manual payment recorded by an owner for exactly one membership. V1 does not initiate or transfer money. A retried HTTP request must not create another payment.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `id` | `uuid` | PK, DEFAULT gen_random_uuid() | Identifier. |
| `membership_id` | `uuid` | NOT NULL, FK -> membership.id | Purchase being paid for. |
| `amount` | `numeric(12,2)` | NOT NULL, CHECK > 0 | Full purchase amount in version 1. |
| `currency` | `varchar(3)` | NOT NULL, CHECK = PLN | Payment currency. |
| `method` | `varchar(20)` | NOT NULL, CHECK | CASH or BANK_TRANSFER. |
| `idempotency_key` | `uuid` | NOT NULL, UNIQUE | Identifies a repeated request to record this payment. |
| `status` | `varchar(20)` | NOT NULL, CHECK | SUCCEEDED only in V1; retain as an explicit financial fact. |
| `recorded_by_user_id` | `uuid` | NOT NULL, FK -> app_user.id | Authenticated owner who recorded the payment. |
| `created_at` | `timestamptz` | NOT NULL | Record creation time. |
| `paid_at` | `timestamptz` | NOT NULL | Server time at confirmation; no backdating in V1. |

## `booking`

One reservation attempt for one member, session and purchased pass. Timely rebooking creates a new row and retains the cancelled attempt.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `id` | `uuid` | PK, DEFAULT gen_random_uuid() | Identifier. |
| `member_id` | `uuid` | NOT NULL, FK -> member_profile.user_id | Participant. |
| `session_id` | `uuid` | NOT NULL, FK -> training_session.id | Dated class. |
| `membership_id` | `uuid` | NOT NULL, composite FK with member_id -> membership(id, member_id) | Pass covering the reservation; the composite FK prevents using another member's pass. |
| `status` | `varchar(20)` | NOT NULL, CHECK | RESERVED, CANCELLED, LATE_CANCELLED, ATTENDED or NO_SHOW. |
| `created_at` | `timestamptz` | NOT NULL | Reservation creation time. |
| `cancelled_at` | `timestamptz` | NULL | Required for CANCELLED and LATE_CANCELLED, otherwise NULL. |
| `cancelled_by_user_id` | `uuid` | NULL, FK -> app_user.id | Actor cancelling the reservation, including an owner cancelling a class. |
| `cancellation_reason` | `text` | NULL | Optional explanation. |
| `attendance_recorded_at` | `timestamptz` | NULL | Required for ATTENDED or NO_SHOW, otherwise NULL. |
| `attendance_recorded_by_user_id` | `uuid` | NULL, FK -> app_user.id | Trainer or owner recording attendance. |

## Required physical constraints

- Primary keys and foreign keys as above; composite primary key `(user_id, role)` on `user_role`.
- `UNIQUE (id, member_id)` on `membership`, referenced by booking's composite FK `(membership_id, member_id)`. This prevents using another member's pass even if application validation regresses.
- Unique index on `booking(member_id, session_id) WHERE status <> 'CANCELLED'`. A timely cancellation permits a new row; late cancellation does not permit rebooking that session.
- `UNIQUE (membership_id)` on payment for V1 manual payments, plus `UNIQUE (idempotency_key)`. A second distinct payment for an already paid pass returns a conflict. Revisit this constraint before adding online providers: a real additional payment must be recorded and reconciled, never silently discarded.
- Explicit status/role CHECKs, positive numbers/amounts, currency PLN, and trimmed non-empty required names. Email normalization occurs before persistence; the unique key uses the normalized stored value.
- `training_session`: ends_at > starts_at; booking_closes_at <= starts_at; free_cancellation_until <= starts_at. CANCELLED iff cancelled_at is populated; cancellation_reason is non-empty when a session is cancelled.
- `membership`: ACTIVE requires valid_from, valid_until and activated_at; PENDING_PAYMENT/CANCELLED require all three to be NULL. valid_until > valid_from whenever dates exist. Write explicit disjunctions/IS NULL conditions: SQL CHECK UNKNOWN is not rejection.
- `booking`: cancelled_at and cancelled_by_user_id are both present exactly for CANCELLED/LATE_CANCELLED. Attendance timestamp and actor are both present exactly for ATTENDED/NO_SHOW. Other states have NULL values in those audit fields. A cancellation reason is optional except for a club cancellation, whose reason is copied from the session.
- Financial/history foreign keys use RESTRICT/NO ACTION. No cascade deletion of users, passes, payments or booking history. No hard-delete API in V1.
- Role/profile consistency is an application transaction invariant: MEMBER iff member profile exists, TRAINER iff trainer profile exists. A foreign key to app_user does not prove role authorization.

## Concurrent schedule conflicts

Use PostgreSQL `btree_gist` and two partial GiST exclusion constraints on scheduled sessions:

- `(room_id WITH =, tstzrange(starts_at, ends_at, '[)') WITH &&)` where status = 'SCHEDULED'.
- `(trainer_id WITH =, tstzrange(starts_at, ends_at, '[)') WITH &&)` under the same predicate.

Adjacent sessions are allowed. These constraints provide the final guard for concurrent overlapping inserts, beyond a friendly application precheck. Create the extension in Flyway using a development/test database role permitted to do so. Verify it with the actual PostgreSQL container. If deployment privileges later differ, provision the extension explicitly; do not silently drop overlap protection.

## Indexes and derived values

Start with indexes supporting actual endpoints: session(starts_at), session(trainer_id, starts_at), booking(session_id, status), booking(membership_id, status), booking(member_id, created_at), membership(member_id, status). Use the real table names from above. Reuse unique/exclusion indexes where appropriate; do not add an index for every column.

Do not persist remaining_entries, seats_available, EXPIRED/EXHAUSTED flags or a scheduled completion flag. Derive these from bookings, status and dates as specified in business-rules.md. No entry ledger is required for the bounded V1 workflows.

## Flyway and JPA

- Flyway owns the schema. Set Hibernate `ddl-auto=validate` and disable Open Session in View.
- Create migrations with the slice that uses them. Suggested sequence: identity, schedule, memberships/payments, bookings. Names/numbers are illustrative until applied.
- Include constraints and indexes with their relevant tables; do not manufacture ALTER migrations to look experienced.
- Never edit an already applied shared versioned migration. Add a real migration for a subsequent change.
- Stable skill-level reference data may be seeded by migration. Demo accounts, prices and bookings belong to a local/demo initializer, not production migrations.
- Use BigDecimal for amounts, Instant for timestamps and LocalDate for pass dates. Store enums as readable strings with matching database CHECKs.
- Avoid JPA inheritance between user/member/trainer. Shared-primary-key profile mappings or explicit IDs are both acceptable; choose the simpler tested mapping.
- Return DTOs, not entities. Use explicit queries/projections or fetch joins for rosters and schedule pages; avoid accidental lazy-loading/N+1 behavior.

## Diagrams and verification

[ER overview](diagrams/schema.mmd) and [editable draw.io diagram](diagrams/training-booking-api.drawio) describe V1. They omit secondary audit-actor links; the column tables and constraints here are authoritative. The legacy diagrams in reference/ include future refunds and are not V1 specifications.

V1__create_room.sql is applied locally and also passed a fresh PostgreSQL Testcontainers startup. The remaining schema is a design, not executed DDL. Parsing diagram XML does not validate future SQL, ORM mappings or concurrency behavior.

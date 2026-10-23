# Incremental delivery plan

Each slice ends with runnable behavior, relevant checks and a short update to [progress.md](progress.md). These are implementation milestones, not claims about work already completed. Do not implement every future idea before showing the first working flow.

## 1. Foundation and identity

Create the single-module Maven project and Wrapper, pin the selected Java/Boot/dependency versions, add PostgreSQL Compose, Flyway, health and basic error handling. Implement accounts, fixed roles/profiles, owner bootstrap, member/trainer account creation, session login/logout/current-user/password change and CSRF. Add initial reference data and opt-in demo credentials via environment.

Acceptance: fresh database migrates, authentication/authorization tests pass, secrets are absent from source, a member cannot create an owner, password change invalidates tracked sessions. Establish CI with the tests that exist; do not add green placeholder workflows.

## 2. Schedule

Implement skill levels, rooms, groups, trainer listing, dated sessions and paginated schedule reads. Apply room/trainer overlap constraints from the start. Catalog updates have the precise limited fields specified in [api.md](../api.md).

Acceptance: owner creates classes; member can browse; room capacity and duplicate times are guarded; adjacent sessions succeed; concurrent conflicts are tested with PostgreSQL. Trainer inactive/archive behavior is clear.

## 3. Passes and manual payments

Implement offers, purchased snapshots, unpaid cancellation, owner receipt recording and atomic activation. Add idempotency and private-resource access control before calling payments complete.

Acceptance: a member buys a pass, an owner records the payment, validity/balance is returned, replay preserves receipt and dates, distinct-key duplicates and cancellation races have deterministic outcomes.

At this point there is a demonstrable partial backend. It may be described as in progress with those implemented behaviors; do not claim bookings or completion yet.

## 4. Booking and attendance

Implement transactional booking, own history, roster views, timely/late cancellation, whole-session cancellation and attendance. Every writer follows the session -> membership lock order.

Acceptance: all concurrency invariants from [testing.md](../testing.md) pass; unauthorized access fails; counts and histories agree after every transition. Run the complete member -> owner payment -> member booking -> trainer attendance flow.

## 5. Portfolio finish

Finish OpenAPI from real endpoints, an executable request walkthrough, application Docker image and full Compose startup. Add focused CI formatting/build/integration checks, run from a clean checkout and finish a concise public README with real commands and current limitations. Include one architecture diagram and a useful API example.

Acceptance: the [scope completion checklist](../scope.md) passes. Record actual local results and separately any remote GitHub Actions results. No online gateway, frontend or cloud deployment is required. Stop feature development unless the user requests new work.

## Handoff format

For each completed slice record:

1. What works now and what still does not exist.
2. Files/migrations/contract decisions changed.
3. Actual verification commands and their outcomes, including unrun or blocked checks.
4. The next single task.
5. Up to three things the user should understand before discussing this code in an interview.

Suggested learning order after the core works: follow one booking request from controller to transaction and SQL; explain pass snapshots and validity; explain the two resource locks; explain CSRF/session ownership; inspect one concurrency test. Do not require an extended course before the next implementation slice.

## Start prompt for an implementation agent

> Work on Spring Boot Boxing Academy API using the repository-root AGENTS.md and documentation indexed in docs/README.md. Inspect the actual files and current progress first. Implement only the next agreed delivery slice, starting with foundation and identity if no code exists. Communicate in Polish and keep repository content in English. Use the selected single-club V1 scope; do not implement online payments, refunds or SaaS. Before editing, give a short plan and the checks you will run. Afterward, update docs/development/progress.md with actual evidence and explain the important decisions. Do not publish or modify my career profiles as part of this task.

This directory is already the `spring-boot-boxing-academy-api` repository root. Create the Maven project here, not inside another nested application directory. Do not reinitialize Git or publish the repository without a corresponding user request.

# Documentation

The current documents specify a planned V1 backend. They do not demonstrate implemented or tested behavior. Use [implementation progress](development/progress.md) for evidence of what actually exists.

## Current specification

| Document | Responsibility |
|---|---|
| [scope.md](scope.md) | Users, features, exclusions and completion criteria. |
| [architecture.md](architecture.md) | Technology, feature packages, security and runtime configuration. |
| [business-rules.md](business-rules.md) | State transitions, deadlines, pass accounting and concurrency. |
| [database.md](database.md) | V1 tables, constraints, indexes and migration guidance. |
| [api.md](api.md) | Frontend-facing API contract, authorization, validation and errors. |
| [testing.md](testing.md) | Required behavior, security and concurrency checks. |
| [decisions.md](decisions.md) | Confirmed choices, recommended defaults and tradeoffs. |

For a quick overview, read scope and architecture. Before implementing a feature, read its relevant business rules, database/API contract and acceptance tests. If documents disagree, resolve the specific conflict rather than implementing both alternatives.

## Current diagrams

- [ER overview](diagrams/schema.mmd): Mermaid source for V1 relationships.
- [Editable database diagram](diagrams/training-booking-api.drawio): open with diagrams.net.

Column definitions and constraints in database.md take precedence over visual shorthand. Diagram structure has been checked mechanically; no visual rendering verification is claimed.

## Development coordination

- [Delivery plan](development/delivery-plan.md): incremental implementation slices and a starting prompt.
- [Progress](development/progress.md): completed work, actual checks, limitations and next task.
- [Agent instructions](../AGENTS.md): collaboration and implementation rules.
- [Code-driven learning notes](../todo.md): optional questions and a walkthrough of the implemented room flow.

These files coordinate implementation; they are not additional product requirements. Keep progress.md concise instead of copying full chat transcripts or command logs into the repository.

## Historical reference

[reference/](reference/README.md) preserves the original broader data model and its diagrams. It includes deferred features and must not be used as the current implementation contract.

## Adding documentation

- Update the existing document that owns a rule rather than duplicating it across files.
- Store current diagram sources in diagrams/ and historical material in reference/.
- Store implementation planning and handoff notes in development/.
- Keep the root README focused on the project, actual capabilities and eventually verified run/test instructions.
- Add new files only when the existing documents no longer provide a clear home for the information. No extra folder hierarchy is needed now.

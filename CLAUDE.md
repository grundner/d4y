# Claude Working Instructions

## Source of Truth

The project is documentation-driven.

The authoritative sources are, in descending order:

1. CLAUDE.md
2. Accepted ADRs in `docs/decisions`
3. Domain documents in `docs/domain`
4. Business rules in `docs/rules`
5. Architectural and design documents in `docs/architecture`
6. Standards and guidelines in `docs/standards`

Code must never contradict the documentation.

If documentation conflicts exist, stop and report them before making changes.

---

## Repository Priorities

The following locations are considered primary project knowledge:

```text
docs/
  standards/
  architecture/
  domain/
  decisions/
  rules/
  ui/

src/
frontend/src/
```

The following locations are considered secondary information and must not override the source of truth:

```text
docs/product/
docs/research/
```

Product vision, research and ideas are never implementation drivers.

---

## Documentation First

Before every implementation task:

1. Run `git diff -- docs/`
2. Identify changed documentation files.
3. Classify each change as:

    * requirement change
    * domain model change
    * decision change
    * clarification
    * idea
    * open question
4. Only implementation-relevant changes from:

    * `docs/domain`
    * `docs/decisions` with `Status: Accepted`
      may affect code.
5. If ambiguity, contradiction or missing information exists:

    * stop implementation
    * report findings
    * request clarification
6. Produce an Implementation Impact Plan.
7. Wait for explicit user instruction before implementing.

Never implement changes automatically based solely on documentation modifications.

---

## Mandatory Impact Analysis

Before changing code, always identify:

* affected domain concepts
* affected ADRs
* affected backend modules
* affected frontend modules
* affected tests
* affected documentation

Output an impact analysis before implementation.

---

## Rules Convention

Business rules follow this convention:

* **Object-specific rules** are documented in the `## Regeln` section of the respective domain object in `docs/domain/`.
* **Cross-cutting rules** that span multiple domain objects are documented in `docs/rules/`.

`docs/rules/` contains only:

* `business-rules.md` — overarching rules not owned by a single domain object
* `privacy-rules.md` — data protection and privacy constraints

Never create a separate rules file for a single domain object. Rules for that object belong in the object's own document.

---

## Domain Driven Development

Business concepts are more important than technical concepts.

Always model and understand:

* domain entities
* value objects
* business rules
* workflows
* relationships

before modifying implementation.

Never introduce technical decisions into domain documents.

---

## ADR Handling

Architectural and domain decisions must be documented as ADRs.

Accepted ADRs are binding.

Rejected or superseded ADRs are historical information only.

Never silently violate an accepted ADR.

If implementation requires deviation from an accepted ADR:

* stop
* explain the conflict
* propose a new ADR

---

## Documentation Maintenance

Whenever implementation changes the behaviour of the system:

* update affected documentation
* update affected ADR references
* update business rules if necessary
* update feature specifications if necessary

Documentation and implementation must remain synchronized.

---

## Implementation Order

Always follow this sequence:

1. Documentation analysis
2. Impact analysis
3. Conflict detection
4. Implementation plan
5. User approval
6. Implementation
7. Tests
8. Documentation updates
9. Final consistency check

Never skip steps.

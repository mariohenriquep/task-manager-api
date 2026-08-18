---
name: architecture-review
description: Reviews a GitHub PR or the current working diff on task-manager-api against docs/architecture.md — onion-architecture layering, domain purity, the domain/entity/DTO three-model separation, SOLID, GRASP, the patterns the project commits to, package placement, and testing conventions. Use whenever the user asks to review a PR, review a diff, or check whether a change follows the architecture/clean-architecture/onion-architecture/SOLID/GRASP conventions for this repo — "review PR #4", "check this diff against our architecture", "does this class belong here", "/architecture-review". Trigger even without the word "architecture" — any request to validate a change's structure, layering, or design against this project's conventions qualifies. This is conformance/design review, not correctness or security — use /code-review or the devsecops-agent for those, alongside this one if useful.
effort: high
---

# architecture-review

Checks a diff against the architecture this project has committed to in `docs/architecture.md`,
not just whether the code works. A change can pass every test and still put a JPA entity in the
domain layer or let a controller decide business logic — correctness review naturally misses
that class of problem because the code "works fine." Unlike a project with no automated
architecture check, this repo also has a mechanical backstop
(`OnionArchitectureTest`, ArchUnit) — run it as a cheap first pass, then do the judgment-based
review this skill is actually for.

## Workflow

### 1. Run the mechanical check first

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw test -Dtest=OnionArchitectureTest
```

If this fails, the layering violation is already proven — cite the specific ArchUnit rule that
broke and skip straight to reporting it as blocking. If it passes, that only clears layering
*direction*; the categories below (three-model separation, SOLID, GRASP, package placement,
pattern conformance) aren't mechanically checked and still need the read-and-judge pass.

### 2. Get the diff

- **PR review:** `gh pr diff <number>`.
- **Working diff (no PR number given):** a plain `git diff` only shows tracked, unstaged
  changes — it silently misses staged changes and, more importantly, brand-new untracked files
  (a fresh `TaskAuditLog.java` won't appear at all, and new classes are exactly what most needs
  reviewing). To see everything a branch actually adds:
  - Committed work on a branch: `git diff <base>...HEAD` (three dots) where `<base>` is `main`
    unless the user says otherwise.
  - Staged but uncommitted: `git diff --staged`.
  - Untracked new files: `git status --porcelain`, then read each one directly.

  If it's ambiguous which of these the user means, ask rather than guessing.

### 3. Read `docs/architecture.md` fresh

Read the whole file before judging anything — don't rely on a remembered version from earlier
in the conversation. It's expected to evolve as the project grows; a stale mental copy produces
a review against rules that no longer apply. If the file is missing, say so and stop rather than
inventing rules from general clean-architecture knowledge.

### 4. Walk the diff against each section

For each one, look for concrete violations in the *changed* code only — don't re-review
unrelated existing code just because it's visible in context, and skip a section entirely if
nothing in the diff touches it rather than forcing a comment to fill out the list.

- **§2 Domain purity** — any framework import/annotation (Spring, JPA, Jackson, Lombok,
  `jakarta.validation`) on a class in `domain/`? This is the single most common way a "quick fix"
  quietly breaks the architecture.
- **§1 Layering** — does `application/` reach into `infrastructure/` directly instead of through
  a port interface it defines in `domain/`? Does a controller hold business logic instead of
  just translating HTTP ↔ use case call?
- **§3 Three models** — is a `TaskJpaEntity` (or any new JPA entity) leaking outside
  `infrastructure.persistence`? Is a domain object serialized straight to the wire instead of
  going through a `web.dto` type? Is there a new type duplicating an existing domain/entity/DTO
  for the same concept instead of reusing it?
- **§4 Package placement** — is every new class in the package the table in §4 says it belongs
  in? A class that doesn't fit any row is worth flagging even if it "works," per §4's own note.
- **§5 Patterns** — new use cases follow the one-class-per-operation shape (`UseCase<IN,OUT>`)?
  A new outbound integration goes through a port + adapter rather than being called directly
  from `application`? Status/workflow logic lives in the aggregate rather than in a service
  deciding what's legal from outside it?
- **§6 SOLID / GRASP** — SRP (a class taking on more than one reason to change), OCP (a new case
  handled by editing an existing branch instead of extending the transition map / adding a new
  type), DIP (`application`/`domain` doing `new SomeConcreteInfrastructureClass()` instead of
  depending on a port).
- **§7 Testing conventions** — is new logic tested at the layer it belongs to (plain JUnit for
  domain/application, `@DataJpaTest`+Testcontainers for persistence, `@WebMvcTest` for web)? Is
  there a real test at all, not just something that executes the line without asserting
  behavior? If JaCoCo's `mvn verify` would drop below its floor because of this diff, say so.

### 5. Report

For each real finding: **file/class → which `docs/architecture.md` section it violates → a
concrete suggested fix**, ranked most-important first — a domain-layer framework leak outranks a
package-placement nit. Separate **blocking** (architecture violations, layering breaks, missing
tests on real logic) from **non-blocking** (nits, style preferences) so the author knows what
must change versus what's optional.

If the change is clean, say so plainly and approve. Don't manufacture nitpicks to look thorough —
an honest "this conforms, ship it" is a valid and valuable review outcome.

## Notes

- This is architecture + design conformance, not general code quality or security. If something
  security-relevant turns up in passing, flag it and point to the `devsecops-agent` rather than
  trying to do a full pass yourself.
- `docs/architecture.md` changing (new layer, revised package table, dropped pattern) changes
  this skill's behavior automatically next run, since the rules are read fresh each time — no
  need to touch this file when only the architecture doc changes.

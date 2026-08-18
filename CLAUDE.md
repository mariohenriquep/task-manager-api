# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

The default `java`/`JAVA_HOME` on this machine is not 21. If `./mvnw` picks the wrong JDK, prefix
every command below with:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

```bash
./mvnw test                                              # full suite (unit + integration + ArchUnit)
./mvnw verify                                            # test + JaCoCo coverage gate (85% line/branch)
./mvnw test -Dtest=TaskTest                              # single test class
./mvnw test -Dtest='TaskTest$Creation#rejectsBlankTitle' # single method (most tests live in @Nested
                                                          # classes - plain TaskTest#method silently
                                                          # runs the whole class instead of filtering)
./mvnw test -Dtest=OnionArchitectureTest                 # architecture rules only
./mvnw spring-boot:run                                   # run the app (needs Postgres, see below)
docker compose up -d                                     # local Postgres for `spring-boot:run`
```

Persistence tests (`TaskRepositoryAdapterTest`) and the full-context smoke test
(`TaskManagerApiApplicationTests`) use Testcontainers, which needs Docker running - they spin up
`postgres:16-alpine` automatically, no manual `docker compose up` needed for `./mvnw test`.

Coverage report after a build: `target/site/jacoco/index.html` (or `jacoco.csv` for a quick
per-class grep). The check binds to `verify`, not `test` - a plain `mvn test` will not fail the
build on a coverage drop.

## Architecture

Onion/clean architecture, three layers, dependencies only point inward
(`infrastructure → application → domain`), enforced by ArchUnit
(`src/test/java/com/taskmanager/api/architecture/OnionArchitectureTest.java`) - not just a
convention, a failing test. **`docs/architecture.md` is the canonical, numbered-section
description of the rules** (layering, domain purity, the three-model separation, package
placement, patterns, SOLID/GRASP application, testing conventions) - read it before making a
structural change, and update it if the structure genuinely changes, since
`.claude/skills/architecture-review` and the `architecture-reviewer` agent read it fresh on
every review rather than encoding rules of their own.

The shape in one paragraph: `domain` (`Task` aggregate, `TaskStatus`, domain exceptions,
`TaskRepository` port) has zero framework dependencies and is pure Java. `application` has one
use case class per operation (`CreateTaskService`, `DeleteTaskService`, ...) implementing
`UseCase<IN, OUT>`, depending only on the `TaskRepository` port. `infrastructure` holds
`web` (`TaskController`, DTOs, `GlobalExceptionHandler`) and `persistence`
(`TaskJpaEntity`, `TaskRepositoryAdapter` implementing the port). `Task` is immutable - every
transition (`start`/`complete`/`reopen`/`updateDetails`) validates and returns a new instance
rather than mutating shared state; the legal-transition table lives in `Task` itself, not in a
service deciding from outside it. Three distinct types exist for "a task"
(`Task` domain / `TaskJpaEntity` JPA / `TaskResponse` web DTO), translated only at
`TaskPersistenceMapper` and `TaskWebMapper` - never pass one across a layer it doesn't belong to.

Test pyramid, one style per layer (mirrored by the `unit-tester` agent):
domain/application tests use plain JUnit 5 + Mockito with **no Spring context**; persistence
tests use `@DataJpaTest` + Testcontainers against real PostgreSQL (`AbstractPostgresIntegrationTest`
is the shared container base class - it's a static field, so the container is reused across every
subclass in the same JVM); web tests use `@WebMvcTest` + MockMvc with use cases mocked.

### Spring Boot 4 gotchas

This targets Spring Boot 4.1.0 (3.5.x hit Maven Central end-of-life June 2026), which
modularized what used to be one `spring-boot-autoconfigure`/`spring-boot-test-autoconfigure` jar
into many small per-technology artifacts, and moved to Jackson 3. If something that "should just
be there" from `spring-boot-starter-test` in a 3.x project doesn't resolve, it's probably one of
these:

- `@DataJpaTest` → `spring-boot-data-jpa-test`, package `org.springframework.boot.data.jpa.test.autoconfigure`
- `@AutoConfigureTestDatabase` → `spring-boot-jdbc-test`, package `org.springframework.boot.jdbc.test.autoconfigure`
- `@WebMvcTest` → `spring-boot-webmvc-test`, package `org.springframework.boot.webmvc.test.autoconfigure`
- `@MockBean` is gone → use `org.springframework.test.context.bean.override.mockito.MockitoBean`
- Flyway autoconfiguration needs `spring-boot-starter-flyway`, not just `flyway-core`
- Jackson: `ObjectMapper` etc. live under `tools.jackson.databind`, not `com.fasterxml.jackson.databind`
- Testcontainers 2.x renamed modules (`testcontainers-junit-jupiter`, `testcontainers-postgresql`)
  and moved `PostgreSQLContainer` to `org.testcontainers.postgresql` (non-generic - no `<?>`)

## GitHub workflow

This repo's git/GitHub conventions are encoded as project skills/agents in `.claude/` - they
trigger automatically, but the one rule worth knowing up front: **every commit must reference a
GitHub issue** (`<type>(<scope>): <subject>, closes|fixes|refs #<N>` - see
`.claude/skills/github-commit`). Find or create the issue before committing if one doesn't exist
yet (`.claude/skills/github-issues`). PRs are opened/updated via `.claude/skills/github-pr` or
the `pr-manager` agent, always ending with the required footer - **no skill or agent in this repo
merges a PR**, that's always a human decision.

Delegatable agents (`.claude/agents/`): `task-developer` (implements features),
`unit-tester` (strengthens coverage against the JaCoCo floor), `architecture-reviewer`
(read-only conformance + quality review), `pr-manager`, `issue-triager`.

## API

`POST/GET/PUT/DELETE /api/tasks`, `PATCH /api/tasks/{id}/status` with body
`{"action": "START"|"COMPLETE"|"REOPEN"}`. Status lifecycle: `TODO → IN_PROGRESS → DONE`, plus
`TODO → DONE` (complete directly) and `DONE → TODO` (reopen); any other transition is
`409 Conflict`, a missing task is `404`, a domain/validation failure is `400`.

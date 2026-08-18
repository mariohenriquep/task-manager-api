# Architecture

This is the source of truth for how task-manager-api is structured. It's read fresh by the
`architecture-review` skill and the `architecture-reviewer` agent on every review — if this
document changes, their behavior changes with it automatically. Keep it accurate; don't let it
drift from what the code actually does.

## 1. Layers and dependency direction

Three concentric layers, dependencies only ever point **inward**:

```
infrastructure  →  application  →  domain
```

- **`domain`** (`com.taskmanager.api.domain`) — the `Task` aggregate, `TaskStatus`, domain
  exceptions, and the `TaskRepository` port interface. Nothing outside this package may be
  imported here.
- **`application`** (`com.taskmanager.api.application`) — one use case per operation
  (`CreateTaskUseCase`/`CreateTaskService`, etc.), plus command records. Depends only on
  `domain`.
- **`infrastructure`** (`com.taskmanager.api.infrastructure`) — everything that talks to the
  outside world: `web` (controllers, DTOs, exception handling), `persistence` (JPA adapter),
  `config`. Depends on both inner layers; nothing may depend on it.

This is mechanically enforced, not just documented: see
`src/test/java/com/taskmanager/api/architecture/OnionArchitectureTest.java` (ArchUnit). A
change that violates layering fails the build, not just this review.

## 2. Domain purity (§2)

`domain/` is plain Java with **zero framework dependencies** — no Spring, no JPA/Hibernate, no
Jackson, no Lombok. `Task` hand-writes its own constructors, accessors, `equals`/`hashCode`.
The business rules must compile and behave the same even if every framework were stripped from
the build. This is what lets domain tests (`TaskTest`) run in milliseconds with no Spring
context.

Framework annotations and DI markers belong in `application` and `infrastructure` only.
`@Service`/`@Component` on application-layer classes is accepted as a pragmatic exception
(it's a DI marker, not a functional dependency) — but nothing from `org.springframework..`,
`jakarta.persistence..`, or `jakarta.validation..` may appear in `domain/`. Enforced by the
`domain_is_free_of_framework_dependencies` ArchUnit rule.

## 3. The three models, never mixed (§3)

A `Task` (domain aggregate), a `TaskJpaEntity` (persistence record), and a `TaskResponse`
(web DTO) are three distinct types for the same concept, deliberately not unified:

- **`Task`** (`domain.model`) — immutable, enforces its own invariants (title length, valid
  status transitions), the only place business rules live.
- **`TaskJpaEntity`** (`infrastructure.persistence.entity`) — mutable, JPA-annotated, exists
  purely to satisfy Hibernate. Never leaves the `persistence` package as itself.
- **`TaskResponse`** / request DTOs (`infrastructure.web.dto`) — the wire shape, decoupled from
  both of the above so the API contract can evolve independently of the schema or the domain
  model.

Translation happens at explicit boundaries only: `TaskPersistenceMapper` (entity ↔ domain) and
`TaskWebMapper` (domain → response). A controller must never return a `TaskJpaEntity`, and a
JPA entity must never appear in `domain/` or be serialized directly.

## 4. Package placement

| What | Package |
|---|---|
| Aggregate, value objects, domain exceptions | `domain.model`, `domain.exception` |
| Repository port (interface only) | `domain.repository` |
| Use case interface + implementation | `application.usecase` |
| Use case input records | `application.command` |
| REST controller | `infrastructure.web.controller` |
| Request/response DTOs, error shape | `infrastructure.web.dto` |
| `@RestControllerAdvice` exception mapping | `infrastructure.web.exception` |
| Domain ↔ DTO translation | `infrastructure.web.mapper` |
| JPA entity | `infrastructure.persistence.entity` |
| Spring Data repository interface | `infrastructure.persistence` (package-private) |
| Repository port implementation (adapter) | `infrastructure.persistence` |
| Entity ↔ domain translation | `infrastructure.persistence.mapper` |
| Spring `@Configuration` beans | `infrastructure.config` |

A new class that doesn't fit one of these rows is a signal to reconsider the design, not to
invent a new package ad hoc — flag it for discussion rather than guessing.

## 5. Patterns in use

- **Repository (port/adapter)** — `domain.repository.TaskRepository` is the port;
  `infrastructure.persistence.TaskRepositoryAdapter` is the only adapter. Application code
  depends on the interface, never on Spring Data or JPA directly (Dependency Inversion).
- **Adapter** — `TaskRepositoryAdapter` adapts Spring Data JPA to the domain's port; the same
  shape would apply to any future outbound integration (an external API, a message queue).
- **Command pattern for use cases** — `UseCase<IN, OUT>` with one implementation class per
  operation (`CreateTaskService`, `DeleteTaskService`, ...). Each has a single reason to
  change (SRP) and is trivially testable in isolation with a mocked `TaskRepository`.
- **Immutable aggregate with self-validating transitions** — `Task.start()`/`complete()`/
  `reopen()`/`updateDetails()` each return a *new* `Task` after checking the move is legal,
  rather than mutating shared state or letting a service class decide what transitions are
  valid. The state machine lives in the aggregate itself (Information Expert, from GRASP).
- **Explicit mapper, not a mapping library** — `TaskPersistenceMapper` and `TaskWebMapper` are
  plain static methods, not MapStruct/ModelMapper. For three fields' worth of aggregates this
  is more debuggable than it is boilerplate; revisit if the domain grows enough that manual
  mapping becomes the actual maintenance burden.

## 6. SOLID / GRASP, applied

- **SRP** — one use case class per operation; the controller only translates HTTP ↔ use case
  call, `GlobalExceptionHandler` only translates exception ↔ HTTP status.
- **OCP** — a new status transition is a new branch in `Task`'s `ALLOWED_TRANSITIONS` map plus
  a new `TaskStatusAction`, not a rewrite of existing transition methods.
- **DIP** — `application` depends on `domain.repository.TaskRepository` (an interface it owns),
  never on `infrastructure.persistence` directly; Spring wires the concrete adapter in at
  runtime.
- **Low coupling / Protected Variations** — the JPA schema (`TaskJpaEntity`, Flyway migrations)
  can change without touching `domain` or `application`, and vice versa, because the mapper is
  the only thing that knows both shapes.

## 7. Testing conventions

- **Domain** — plain JUnit 5, no Spring context (`TaskTest`). Milliseconds, no I/O.
- **Application** — JUnit 5 + Mockito, `TaskRepository` mocked (`*ServiceTest`). No Spring
  context.
- **Persistence** — `@DataJpaTest` + Testcontainers against real PostgreSQL
  (`TaskRepositoryAdapterTest`), so the Flyway migration and JPA mapping are verified against
  the real database engine, not an in-memory stand-in.
- **Web** — `@WebMvcTest` + MockMvc, use cases mocked (`TaskControllerTest`).
- **Architecture** — ArchUnit (`OnionArchitectureTest`), enforcing everything in §1–§2
  mechanically rather than relying on review alone to catch a layering violation.
- **Coverage** — enforced via JaCoCo (`pom.xml`); see the badge/threshold configured there for
  the current target. Coverage is a means, not the goal: a test that executes a line without
  asserting real behavior doesn't count, regardless of what the percentage says.

New code should follow the same TDD flow this codebase was built with: write the failing test
first, at the layer the logic actually belongs to (per §1), then implement the minimum to pass
it.

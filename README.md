# task-manager-api

A Task Manager REST API built as a reference implementation of **OOP + TDD + onion/clean architecture** in Java.

## Stack

- **Java 21**
- **Spring Boot 4.1.0** (Spring Framework 7, Jackson 3)
- **PostgreSQL** + **Flyway** for schema migrations
- **JUnit 5**, **Mockito**, **AssertJ**, **Testcontainers**, **ArchUnit**
- **Maven** (with wrapper: `./mvnw`)

## Architecture

The codebase is organized in three concentric layers. Dependencies only ever point **inward**,
towards the domain - enforced automatically by [`OnionArchitectureTest`](src/test/java/com/taskmanager/api/architecture/OnionArchitectureTest.java).

```
com.taskmanager.api
├── domain            core aggregate, value objects, domain exceptions, repository port
│   ├── model             Task (immutable aggregate), TaskStatus
│   ├── exception          InvalidTaskException, InvalidTaskStatusTransitionException, TaskNotFoundException
│   └── repository         TaskRepository (port interface - no implementation here)
│
├── application       use cases orchestrating the domain, one class per operation
│   ├── usecase             CreateTaskUseCase/Service, GetTaskUseCase/Service, ...
│   └── command             CreateTaskCommand, UpdateTaskCommand, ChangeTaskStatusCommand
│
└── infrastructure    everything that talks to the outside world
    ├── web                 TaskController, request/response DTOs, GlobalExceptionHandler
    ├── persistence         TaskJpaEntity, TaskJpaRepository, TaskRepositoryAdapter (implements the port)
    └── config              ClockConfig (injectable Clock, for deterministic tests)
```

**Why this shape:**
- `domain` has **zero framework dependencies** - no Spring, no JPA, nothing. It is plain Java,
  fast to test, and would survive a rewrite of every other layer.
- `application` depends only on `domain`. Each use case is a small class with a single
  `execute(...)` method (`UseCase<IN, OUT>`), making tests trivial: mock the `TaskRepository`
  port, assert on the returned `Task`.
- `infrastructure` depends on both inner layers but never the other way around. The JPA entity
  (`TaskJpaEntity`) and the domain aggregate (`Task`) are deliberately different classes,
  translated by `TaskPersistenceMapper` - the database schema can evolve without touching
  business logic, and vice versa.
- `Task` itself is an immutable aggregate: every transition (`start`, `complete`, `reopen`,
  `updateDetails`) returns a new instance after validating the move is legal, instead of
  mutating shared state.

A pragmatic compromise: application-layer services carry `@Service`/`@Component` for
Spring wiring. That's a DI annotation, not a functional dependency - the alternative (wiring
everything through `@Configuration` classes to keep application 100% framework-free) is a
valid stricter variant if you want to take it further.

## Running it

Start a local Postgres:

```bash
docker compose up -d
```

Run the app:

```bash
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080/api/tasks`.

## Testing

```bash
./mvnw test
```

This runs the full pyramid:
- **Domain unit tests** (`TaskTest`) - pure, no Spring context, milliseconds.
- **Application unit tests** (`*ServiceTest`) - Mockito-mocked `TaskRepository`, no Spring context.
- **Persistence integration tests** (`TaskRepositoryAdapterTest`) - real PostgreSQL via
  Testcontainers, real Flyway migration, `@DataJpaTest` slice.
- **Web layer tests** (`TaskControllerTest`) - `@WebMvcTest` + MockMvc, use cases mocked.
- **Architecture tests** (`OnionArchitectureTest`) - ArchUnit rules that fail the build if a
  layer boundary is crossed.
- **Full context smoke test** (`TaskManagerApiApplicationTests`) - the whole app wired
  against a real database.

Persistence and full-context tests need Docker running (Testcontainers spins up
`postgres:16-alpine` automatically; no manual setup required).

## API

| Method | Path                    | Description                          |
|--------|-------------------------|---------------------------------------|
| POST   | `/api/tasks`             | Create a task                        |
| GET    | `/api/tasks`             | List all tasks                       |
| GET    | `/api/tasks/{id}`        | Get a task by id                     |
| PUT    | `/api/tasks/{id}`        | Update title/description/due date    |
| PATCH  | `/api/tasks/{id}/status` | Transition status (`START`/`COMPLETE`/`REOPEN`) |
| DELETE | `/api/tasks/{id}`        | Delete a task                        |

Task status lifecycle: `TODO → IN_PROGRESS → DONE`, with `DONE → TODO` (reopen) allowed;
`TODO → DONE` (complete directly) is also allowed. Any other transition returns `409 Conflict`.

## Notable version choices

This project targets **Spring Boot 4.1.0**, the current actively-supported line (3.5.x reached
its Maven Central end-of-life in June 2026). Boot 4 modularized what used to be one big
`spring-boot-autoconfigure`/`spring-boot-test-autoconfigure` jar into many small per-technology
modules, and moved to Jackson 3. Concretely, this project depends on:
`spring-boot-starter-flyway`, `spring-boot-data-jpa-test`, `spring-boot-webmvc-test`, and uses
`tools.jackson.databind.ObjectMapper` (Jackson 3) rather than `com.fasterxml.jackson.databind`
in tests.

## Not included (yet)

Authentication/authorization was intentionally left out of this first slice to keep the focus
on the Task domain and the architecture. Adding it later means a new `User` aggregate plus a
Spring Security filter chain in `infrastructure` - it shouldn't require touching `domain` or
`application` for the Task use cases themselves.

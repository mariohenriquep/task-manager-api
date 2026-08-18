package com.taskmanager.api.domain.model;

import com.taskmanager.api.domain.exception.InvalidTaskException;
import com.taskmanager.api.domain.exception.InvalidTaskStatusTransitionException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The Task aggregate: the single entry point for reading and mutating a task's state.
 *
 * <p>Instances are immutable - every "mutation" (starting, completing, reopening, editing)
 * returns a new {@code Task} instance rather than changing the receiver in place. This keeps
 * the aggregate side-effect free and trivial to unit test.
 */
public final class Task {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            TaskStatus.TODO, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE),
            TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.DONE),
            TaskStatus.DONE, EnumSet.of(TaskStatus.TODO)
    );

    private final UUID id;
    private final String title;
    private final String description;
    private final TaskStatus status;
    private final LocalDate dueDate;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Task(UUID id, String title, String description, TaskStatus status,
                 LocalDate dueDate, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Creates a brand-new task, in {@link TaskStatus#TODO}, using the given clock for timestamps. */
    public static Task create(String title, String description, LocalDate dueDate, Clock clock) {
        Instant now = clock.instant();
        return new Task(
                UUID.randomUUID(),
                requireValidTitle(title),
                requireValidDescription(description),
                TaskStatus.TODO,
                dueDate,
                now,
                now
        );
    }

    /** Creates a brand-new task using the system clock. Convenience overload for callers that don't need control over time. */
    public static Task create(String title, String description, LocalDate dueDate) {
        return create(title, description, dueDate, Clock.systemUTC());
    }

    /** Rebuilds a task from previously persisted data, without re-applying creation defaults. */
    public static Task reconstruct(UUID id, String title, String description, TaskStatus status,
                                    LocalDate dueDate, Instant createdAt, Instant updatedAt) {
        return new Task(
                Objects.requireNonNull(id, "id must not be null"),
                requireValidTitle(title),
                requireValidDescription(description),
                Objects.requireNonNull(status, "status must not be null"),
                dueDate,
                Objects.requireNonNull(createdAt, "createdAt must not be null"),
                Objects.requireNonNull(updatedAt, "updatedAt must not be null")
        );
    }

    public Task start(Clock clock) {
        return transitionTo(TaskStatus.IN_PROGRESS, clock);
    }

    public Task complete(Clock clock) {
        return transitionTo(TaskStatus.DONE, clock);
    }

    public Task reopen(Clock clock) {
        return transitionTo(TaskStatus.TODO, clock);
    }

    private Task transitionTo(TaskStatus target, Clock clock) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(status, Set.of()).contains(target)) {
            throw new InvalidTaskStatusTransitionException(status, target);
        }
        return new Task(id, title, description, target, dueDate, createdAt, clock.instant());
    }

    public Task updateDetails(String title, String description, LocalDate dueDate, Clock clock) {
        return new Task(
                id,
                requireValidTitle(title),
                requireValidDescription(description),
                status,
                dueDate,
                createdAt,
                clock.instant()
        );
    }

    private static String requireValidTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskException("Task title must not be blank");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new InvalidTaskException("Task title must not exceed " + MAX_TITLE_LENGTH + " characters");
        }
        return title;
    }

    private static String requireValidDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidTaskException("Task description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return description;
    }

    public UUID id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public TaskStatus status() {
        return status;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Task{id=%s, title='%s', status=%s}".formatted(id, title, status);
    }
}

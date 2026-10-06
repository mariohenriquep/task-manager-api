package com.taskmanager.api.domain.model;

import com.taskmanager.api.domain.exception.InvalidTaskException;
import com.taskmanager.api.domain.exception.InvalidTaskStatusTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);

    @Nested
    class Creation {

        @Test
        void createsTaskWithValidData() {
            Task task = Task.create("Write tests first", "Follow TDD", LocalDate.of(2026, 9, 1), FIXED_CLOCK);

            assertThat(task.id()).isNotNull();
            assertThat(task.title()).isEqualTo("Write tests first");
            assertThat(task.description()).isEqualTo("Follow TDD");
            assertThat(task.status()).isEqualTo(TaskStatus.TODO);
            assertThat(task.dueDate()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(task.createdAt()).isEqualTo(FIXED_CLOCK.instant());
            assertThat(task.updatedAt()).isEqualTo(FIXED_CLOCK.instant());
        }

        @Test
        void allowsNullDescriptionAndDueDate() {
            Task task = Task.create("Title only", null, null, FIXED_CLOCK);

            assertThat(task.description()).isNull();
            assertThat(task.dueDate()).isNull();
        }

        @Test
        void rejectsBlankTitle() {
            InvalidTaskException ex = assertThrows(InvalidTaskException.class,
                    () -> Task.create("   ", "desc", null, FIXED_CLOCK));

            assertThat(ex.getMessage()).contains("title");
        }

        @Test
        void rejectsNullTitle() {
            InvalidTaskException ex = assertThrows(InvalidTaskException.class,
                    () -> Task.create(null, "desc", null, FIXED_CLOCK));

            assertThat(ex.getMessage()).contains("title");
        }

        @Test
        void acceptsTitleOfExactly200Characters() {
            String maxTitle = "a".repeat(200);

            Task task = assertDoesNotThrow(() -> Task.create(maxTitle, "desc", null, FIXED_CLOCK));

            assertThat(task.title()).hasSize(200);
        }

        @Test
        void acceptsDescriptionOfExactly2000Characters() {
            String maxDescription = "a".repeat(2000);

            Task task = assertDoesNotThrow(() -> Task.create("Title", maxDescription, null, FIXED_CLOCK));

            assertThat(task.description()).hasSize(2000);
        }

        @Test
        void rejectsTitleLongerThan200Characters() {
            String tooLong = "a".repeat(201);

            InvalidTaskException ex = assertThrows(InvalidTaskException.class,
                    () -> Task.create(tooLong, "desc", null, FIXED_CLOCK));

            assertThat(ex.getMessage()).contains("200");
        }

        @Test
        void rejectsDescriptionLongerThan2000Characters() {
            String tooLong = "a".repeat(2001);

            InvalidTaskException ex = assertThrows(InvalidTaskException.class,
                    () -> Task.create("Title", tooLong, null, FIXED_CLOCK));

            assertThat(ex.getMessage()).contains("2000");
        }

        @Test
        void systemClockOverloadStampsCurrentTime() {
            Instant before = Instant.now();

            Task task = Task.create("Title", null, null);

            assertThat(task.createdAt()).isBetween(before, Instant.now());
            assertThat(task.updatedAt()).isEqualTo(task.createdAt());
        }
    }

    @Nested
    class Identity {

        @Test
        void tasksWithTheSameIdAreEqualEvenIfOtherFieldsDiffer() {
            Task original = Task.create("Title", null, null, FIXED_CLOCK);
            Task laterVersion = original.updateDetails("Different title", "desc", null, FIXED_CLOCK);

            assertThat(laterVersion).isEqualTo(original);
            assertThat(laterVersion).hasSameHashCodeAs(original);
        }

        @Test
        void tasksWithDifferentIdsAreNeverEqual() {
            Task first = Task.create("Same title", null, null, FIXED_CLOCK);
            Task second = Task.create("Same title", null, null, FIXED_CLOCK);

            assertThat(first).isNotEqualTo(second);
        }

        @Test
        void aTaskIsNotEqualToSomeOtherType() {
            Task task = Task.create("Title", null, null, FIXED_CLOCK);

            assertThat(task).isNotEqualTo("not a task");
        }

        @Test
        void toStringIncludesIdTitleAndStatus() {
            Task task = Task.create("Write docs", null, null, FIXED_CLOCK);

            assertThat(task.toString())
                    .contains(task.id().toString())
                    .contains("Write docs")
                    .contains("TODO");
        }
    }

    @Nested
    class StatusTransitions {

        @Test
        void startMovesTaskFromTodoToInProgress() {
            Task task = Task.create("Task", null, null, FIXED_CLOCK);

            Task started = task.start(FIXED_CLOCK);

            assertThat(started.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        void startFailsWhenTaskIsNotTodo() {
            Task task = Task.create("Task", null, null, FIXED_CLOCK).start(FIXED_CLOCK);

            InvalidTaskStatusTransitionException ex = assertThrows(InvalidTaskStatusTransitionException.class,
                    () -> task.start(FIXED_CLOCK));

            assertThat(ex.getMessage()).contains("IN_PROGRESS to IN_PROGRESS");
        }

        @Test
        void completeMovesTaskFromInProgressToDone() {
            Task task = Task.create("Task", null, null, FIXED_CLOCK).start(FIXED_CLOCK);

            Task completed = task.complete(FIXED_CLOCK);

            assertThat(completed.status()).isEqualTo(TaskStatus.DONE);
        }

        @Test
        void completeMovesTaskFromTodoToDone() {
            Task task = Task.create("Task", null, null, FIXED_CLOCK);

            Task completed = task.complete(FIXED_CLOCK);

            assertThat(completed.status()).isEqualTo(TaskStatus.DONE);
        }

        @Test
        void completeFailsWhenTaskIsAlreadyDone() {
            Task task = Task.create("Task", null, null, FIXED_CLOCK).complete(FIXED_CLOCK);

            InvalidTaskStatusTransitionException ex = assertThrows(InvalidTaskStatusTransitionException.class,
                    () -> task.complete(FIXED_CLOCK));

            assertThat(ex.getMessage()).contains("DONE to DONE");
        }

        @Test
        void reopenMovesTaskFromDoneToTodo() {
            Task task = Task.create("Task", null, null, FIXED_CLOCK).complete(FIXED_CLOCK);

            Task reopened = task.reopen(FIXED_CLOCK);

            assertThat(reopened.status()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        void reopenFailsWhenTaskIsNotDone() {
            Task task = Task.create("Task", null, null, FIXED_CLOCK);

            InvalidTaskStatusTransitionException ex = assertThrows(InvalidTaskStatusTransitionException.class,
                    () -> task.reopen(FIXED_CLOCK));

            assertThat(ex.getMessage()).contains("TODO to TODO");
        }

        @ParameterizedTest(name = "{0} is rejected from {1} (target {2})")
        @MethodSource("illegalTransitions")
        void illegalTransitionsAreRejectedWithFromAndToInTheMessage(
                String action, TaskStatus from, TaskStatus to) {
            Task task = taskInStatus(from);

            InvalidTaskStatusTransitionException ex = assertThrows(InvalidTaskStatusTransitionException.class,
                    () -> apply(action, task));

            assertThat(ex.getMessage())
                    .isEqualTo("Cannot move task from status " + from + " to " + to);
        }

        static Stream<Arguments> illegalTransitions() {
            return Stream.of(
                    Arguments.of("START", TaskStatus.IN_PROGRESS, TaskStatus.IN_PROGRESS),
                    Arguments.of("START", TaskStatus.DONE, TaskStatus.IN_PROGRESS),
                    Arguments.of("COMPLETE", TaskStatus.DONE, TaskStatus.DONE),
                    Arguments.of("REOPEN", TaskStatus.TODO, TaskStatus.TODO),
                    Arguments.of("REOPEN", TaskStatus.IN_PROGRESS, TaskStatus.TODO)
            );
        }

        private Task taskInStatus(TaskStatus status) {
            Task todo = Task.create("Task", null, null, FIXED_CLOCK);
            return switch (status) {
                case TODO -> todo;
                case IN_PROGRESS -> todo.start(FIXED_CLOCK);
                case DONE -> todo.complete(FIXED_CLOCK);
            };
        }

        private Task apply(String action, Task task) {
            return switch (action) {
                case "START" -> task.start(FIXED_CLOCK);
                case "COMPLETE" -> task.complete(FIXED_CLOCK);
                case "REOPEN" -> task.reopen(FIXED_CLOCK);
                default -> throw new IllegalArgumentException("Unknown action " + action);
            };
        }

        @Test
        void transitionsTouchUpdatedAt() {
            Clock later = Clock.fixed(FIXED_CLOCK.instant().plusSeconds(60), ZoneOffset.UTC);
            Task task = Task.create("Task", null, null, FIXED_CLOCK);

            Task started = task.start(later);

            assertThat(started.updatedAt()).isEqualTo(later.instant());
            assertThat(started.createdAt()).isEqualTo(FIXED_CLOCK.instant());
        }
    }

    @Nested
    class UpdatingDetails {

        @Test
        void updatesTitleDescriptionAndDueDate() {
            Clock later = Clock.fixed(FIXED_CLOCK.instant().plusSeconds(60), ZoneOffset.UTC);
            Task task = Task.create("Original", "Original desc", null, FIXED_CLOCK);

            Task updated = task.updateDetails("New title", "New desc", LocalDate.of(2026, 10, 1), later);

            assertThat(updated.title()).isEqualTo("New title");
            assertThat(updated.description()).isEqualTo("New desc");
            assertThat(updated.dueDate()).isEqualTo(LocalDate.of(2026, 10, 1));
            assertThat(updated.updatedAt()).isEqualTo(later.instant());
            assertThat(updated.status()).isEqualTo(task.status());
        }

        @Test
        void rejectsBlankTitleOnUpdate() {
            Task task = Task.create("Original", null, null, FIXED_CLOCK);

            InvalidTaskException ex = assertThrows(InvalidTaskException.class,
                    () -> task.updateDetails("  ", null, null, FIXED_CLOCK));

            assertThat(ex.getMessage()).contains("title");
        }
    }

    @Nested
    class Reconstruction {

        private final UUID id = UUID.randomUUID();
        private final Instant createdAt = FIXED_CLOCK.instant();
        private final Instant updatedAt = FIXED_CLOCK.instant().plusSeconds(60);

        @Test
        void rebuildsTaskFromPersistedDataWithoutChangingIt() {
            Task task = Task.reconstruct(id, "Title", "desc", TaskStatus.IN_PROGRESS,
                    LocalDate.of(2026, 9, 1), createdAt, updatedAt);

            assertThat(task.id()).isEqualTo(id);
            assertThat(task.status()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(task.createdAt()).isEqualTo(createdAt);
            assertThat(task.updatedAt()).isEqualTo(updatedAt);
        }

        @ParameterizedTest(name = "null {0} is rejected")
        @MethodSource("nullArguments")
        void rejectsNullRequiredArgument(String argument, boolean nullId, boolean nullStatus,
                                         boolean nullCreatedAt, boolean nullUpdatedAt) {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> Task.reconstruct(
                    nullId ? null : id,
                    "Title",
                    "desc",
                    nullStatus ? null : TaskStatus.TODO,
                    null,
                    nullCreatedAt ? null : createdAt,
                    nullUpdatedAt ? null : updatedAt));

            assertThat(ex.getMessage()).isEqualTo(argument + " must not be null");
        }

        static Stream<Arguments> nullArguments() {
            return Stream.of(
                    Arguments.of("id", true, false, false, false),
                    Arguments.of("status", false, true, false, false),
                    Arguments.of("createdAt", false, false, true, false),
                    Arguments.of("updatedAt", false, false, false, true)
            );
        }

        @Test
        void rejectsBlankTitleWhenReconstructing() {
            InvalidTaskException ex = assertThrows(InvalidTaskException.class, () -> Task.reconstruct(
                    id, " ", "desc", TaskStatus.TODO, null, createdAt, updatedAt));

            assertThat(ex.getMessage()).contains("title");
        }
    }
}

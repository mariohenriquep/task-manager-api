package com.taskmanager.api.domain.model;

import com.taskmanager.api.domain.exception.InvalidTaskException;
import com.taskmanager.api.domain.exception.InvalidTaskStatusTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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
            // Arrange
            String title = "Write tests first";
            String description = "Follow TDD";
            LocalDate dueDate = LocalDate.of(2026, 9, 1);

            // Act
            Task task = Task.create(title, description, dueDate, FIXED_CLOCK);

            // Assert
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
            // Arrange
            String title = "Title only";

            // Act
            Task task = Task.create(title, null, null, FIXED_CLOCK);

            // Assert
            assertThat(task.description()).isNull();
            assertThat(task.dueDate()).isNull();
        }

        @Test
        void rejectsBlankTitle() {
            // Arrange
            Executable act = () -> Task.create("   ", "desc", null, FIXED_CLOCK);

            // Act
            InvalidTaskException ex = assertThrows(InvalidTaskException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("title");
        }

        @Test
        void rejectsNullTitle() {
            // Arrange
            Executable act = () -> Task.create(null, "desc", null, FIXED_CLOCK);

            // Act
            InvalidTaskException ex = assertThrows(InvalidTaskException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("title");
        }

        @Test
        void acceptsTitleOfExactly200Characters() {
            // Arrange
            String maxTitle = "a".repeat(200);

            // Act
            Task task = assertDoesNotThrow(() -> Task.create(maxTitle, "desc", null, FIXED_CLOCK));

            // Assert
            assertThat(task.title()).hasSize(200);
        }

        @Test
        void acceptsDescriptionOfExactly2000Characters() {
            // Arrange
            String maxDescription = "a".repeat(2000);

            // Act
            Task task = assertDoesNotThrow(() -> Task.create("Title", maxDescription, null, FIXED_CLOCK));

            // Assert
            assertThat(task.description()).hasSize(2000);
        }

        @Test
        void rejectsTitleLongerThan200Characters() {
            // Arrange
            String tooLong = "a".repeat(201);
            Executable act = () -> Task.create(tooLong, "desc", null, FIXED_CLOCK);

            // Act
            InvalidTaskException ex = assertThrows(InvalidTaskException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("200");
        }

        @Test
        void rejectsDescriptionLongerThan2000Characters() {
            // Arrange
            String tooLong = "a".repeat(2001);
            Executable act = () -> Task.create("Title", tooLong, null, FIXED_CLOCK);

            // Act
            InvalidTaskException ex = assertThrows(InvalidTaskException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("2000");
        }

        @Test
        void systemClockOverloadStampsCurrentTime() {
            // Arrange
            Instant before = Instant.now();

            // Act
            Task task = Task.create("Title", null, null);

            // Assert
            assertThat(task.createdAt()).isBetween(before, Instant.now());
            assertThat(task.updatedAt()).isEqualTo(task.createdAt());
        }
    }

    @Nested
    class Identity {

        @Test
        void tasksWithTheSameIdAreEqualEvenIfOtherFieldsDiffer() {
            // Arrange
            Task original = Task.create("Title", null, null, FIXED_CLOCK);

            // Act
            Task laterVersion = original.updateDetails("Different title", "desc", null, FIXED_CLOCK);

            // Assert
            assertThat(laterVersion).isEqualTo(original);
            assertThat(laterVersion).hasSameHashCodeAs(original);
        }

        @Test
        void tasksWithDifferentIdsAreNeverEqual() {
            // Arrange
            Task first = Task.create("Same title", null, null, FIXED_CLOCK);

            // Act
            Task second = Task.create("Same title", null, null, FIXED_CLOCK);

            // Assert
            assertThat(first).isNotEqualTo(second);
        }

        @Test
        void aTaskIsNotEqualToSomeOtherType() {
            // Arrange
            Task task = Task.create("Title", null, null, FIXED_CLOCK);
            Object other = "not a task";

            // Act
            boolean equal = task.equals(other);

            // Assert
            assertThat(equal).isFalse();
        }

        @Test
        void toStringIncludesIdTitleAndStatus() {
            // Arrange
            Task task = Task.create("Write docs", null, null, FIXED_CLOCK);

            // Act
            String text = task.toString();

            // Assert
            assertThat(text)
                    .contains(task.id().toString())
                    .contains("Write docs")
                    .contains("TODO");
        }
    }

    @Nested
    class StatusTransitions {

        @Test
        void startMovesTaskFromTodoToInProgress() {
            // Arrange
            Task task = Task.create("Task", null, null, FIXED_CLOCK);

            // Act
            Task started = task.start(FIXED_CLOCK);

            // Assert
            assertThat(started.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        void startFailsWhenTaskIsNotTodo() {
            // Arrange
            Task task = Task.create("Task", null, null, FIXED_CLOCK).start(FIXED_CLOCK);
            Executable act = () -> task.start(FIXED_CLOCK);

            // Act
            InvalidTaskStatusTransitionException ex =
                    assertThrows(InvalidTaskStatusTransitionException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("IN_PROGRESS to IN_PROGRESS");
        }

        @Test
        void completeMovesTaskFromInProgressToDone() {
            // Arrange
            Task task = Task.create("Task", null, null, FIXED_CLOCK).start(FIXED_CLOCK);

            // Act
            Task completed = task.complete(FIXED_CLOCK);

            // Assert
            assertThat(completed.status()).isEqualTo(TaskStatus.DONE);
        }

        @Test
        void completeMovesTaskFromTodoToDone() {
            // Arrange
            Task task = Task.create("Task", null, null, FIXED_CLOCK);

            // Act
            Task completed = task.complete(FIXED_CLOCK);

            // Assert
            assertThat(completed.status()).isEqualTo(TaskStatus.DONE);
        }

        @Test
        void completeFailsWhenTaskIsAlreadyDone() {
            // Arrange
            Task task = Task.create("Task", null, null, FIXED_CLOCK).complete(FIXED_CLOCK);
            Executable act = () -> task.complete(FIXED_CLOCK);

            // Act
            InvalidTaskStatusTransitionException ex =
                    assertThrows(InvalidTaskStatusTransitionException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("DONE to DONE");
        }

        @Test
        void reopenMovesTaskFromDoneToTodo() {
            // Arrange
            Task task = Task.create("Task", null, null, FIXED_CLOCK).complete(FIXED_CLOCK);

            // Act
            Task reopened = task.reopen(FIXED_CLOCK);

            // Assert
            assertThat(reopened.status()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        void reopenFailsWhenTaskIsNotDone() {
            // Arrange
            Task task = Task.create("Task", null, null, FIXED_CLOCK);
            Executable act = () -> task.reopen(FIXED_CLOCK);

            // Act
            InvalidTaskStatusTransitionException ex =
                    assertThrows(InvalidTaskStatusTransitionException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("TODO to TODO");
        }

        @ParameterizedTest(name = "{0} is rejected from {1} (target {2})")
        @MethodSource("illegalTransitions")
        void illegalTransitionsAreRejectedWithFromAndToInTheMessage(
                String action, TaskStatus from, TaskStatus to) {
            // Arrange
            Task task = taskInStatus(from);
            Executable act = () -> apply(action, task);

            // Act
            InvalidTaskStatusTransitionException ex =
                    assertThrows(InvalidTaskStatusTransitionException.class, act);

            // Assert
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
            // Arrange
            Clock later = Clock.fixed(FIXED_CLOCK.instant().plusSeconds(60), ZoneOffset.UTC);
            Task task = Task.create("Task", null, null, FIXED_CLOCK);

            // Act
            Task started = task.start(later);

            // Assert
            assertThat(started.updatedAt()).isEqualTo(later.instant());
            assertThat(started.createdAt()).isEqualTo(FIXED_CLOCK.instant());
        }
    }

    @Nested
    class UpdatingDetails {

        @Test
        void updatesTitleDescriptionAndDueDate() {
            // Arrange
            Clock later = Clock.fixed(FIXED_CLOCK.instant().plusSeconds(60), ZoneOffset.UTC);
            Task task = Task.create("Original", "Original desc", null, FIXED_CLOCK);

            // Act
            Task updated = task.updateDetails("New title", "New desc", LocalDate.of(2026, 10, 1), later);

            // Assert
            assertThat(updated.title()).isEqualTo("New title");
            assertThat(updated.description()).isEqualTo("New desc");
            assertThat(updated.dueDate()).isEqualTo(LocalDate.of(2026, 10, 1));
            assertThat(updated.updatedAt()).isEqualTo(later.instant());
            assertThat(updated.status()).isEqualTo(task.status());
        }

        @Test
        void rejectsBlankTitleOnUpdate() {
            // Arrange
            Task task = Task.create("Original", null, null, FIXED_CLOCK);
            Executable act = () -> task.updateDetails("  ", null, null, FIXED_CLOCK);

            // Act
            InvalidTaskException ex = assertThrows(InvalidTaskException.class, act);

            // Assert
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
            // Arrange
            LocalDate dueDate = LocalDate.of(2026, 9, 1);

            // Act
            Task task = Task.reconstruct(id, "Title", "desc", TaskStatus.IN_PROGRESS,
                    dueDate, createdAt, updatedAt);

            // Assert
            assertThat(task.id()).isEqualTo(id);
            assertThat(task.status()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(task.createdAt()).isEqualTo(createdAt);
            assertThat(task.updatedAt()).isEqualTo(updatedAt);
        }

        @ParameterizedTest(name = "null {0} is rejected")
        @MethodSource("nullArguments")
        void rejectsNullRequiredArgument(String argument, boolean nullId, boolean nullStatus,
                                         boolean nullCreatedAt, boolean nullUpdatedAt) {
            // Arrange
            Executable act = () -> Task.reconstruct(
                    nullId ? null : id,
                    "Title",
                    "desc",
                    nullStatus ? null : TaskStatus.TODO,
                    null,
                    nullCreatedAt ? null : createdAt,
                    nullUpdatedAt ? null : updatedAt);

            // Act
            NullPointerException ex = assertThrows(NullPointerException.class, act);

            // Assert
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
            // Arrange
            Executable act = () -> Task.reconstruct(
                    id, " ", "desc", TaskStatus.TODO, null, createdAt, updatedAt);

            // Act
            InvalidTaskException ex = assertThrows(InvalidTaskException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("title");
        }
    }
}

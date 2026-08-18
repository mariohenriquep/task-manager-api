package com.taskmanager.api.domain.model;

import com.taskmanager.api.domain.exception.InvalidTaskException;
import com.taskmanager.api.domain.exception.InvalidTaskStatusTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            assertThatThrownBy(() -> Task.create("   ", "desc", null, FIXED_CLOCK))
                    .isInstanceOf(InvalidTaskException.class)
                    .hasMessageContaining("title");
        }

        @Test
        void rejectsTitleLongerThan200Characters() {
            String tooLong = "a".repeat(201);

            assertThatThrownBy(() -> Task.create(tooLong, "desc", null, FIXED_CLOCK))
                    .isInstanceOf(InvalidTaskException.class)
                    .hasMessageContaining("200");
        }

        @Test
        void rejectsDescriptionLongerThan2000Characters() {
            String tooLong = "a".repeat(2001);

            assertThatThrownBy(() -> Task.create("Title", tooLong, null, FIXED_CLOCK))
                    .isInstanceOf(InvalidTaskException.class)
                    .hasMessageContaining("2000");
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

            assertThatThrownBy(() -> task.start(FIXED_CLOCK))
                    .isInstanceOf(InvalidTaskStatusTransitionException.class);
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

            assertThatThrownBy(() -> task.complete(FIXED_CLOCK))
                    .isInstanceOf(InvalidTaskStatusTransitionException.class);
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

            assertThatThrownBy(() -> task.reopen(FIXED_CLOCK))
                    .isInstanceOf(InvalidTaskStatusTransitionException.class);
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

            assertThatThrownBy(() -> task.updateDetails("  ", null, null, FIXED_CLOCK))
                    .isInstanceOf(InvalidTaskException.class);
        }
    }
}

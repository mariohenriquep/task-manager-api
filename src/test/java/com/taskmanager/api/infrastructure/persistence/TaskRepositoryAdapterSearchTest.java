package com.taskmanager.api.infrastructure.persistence;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.model.TaskStatus;
import com.taskmanager.api.domain.query.Page;
import com.taskmanager.api.domain.query.PageRequest;
import com.taskmanager.api.domain.query.SortDirection;
import com.taskmanager.api.domain.query.TaskQuery;
import com.taskmanager.api.domain.query.TaskSort;
import com.taskmanager.api.domain.query.TaskSortField;
import com.taskmanager.api.infrastructure.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link TaskRepositoryAdapter#search} against real PostgreSQL. Every fixture has a fixed
 * id, createdAt and due date, so assertions are on exact ids and exact order. Each test rolls back,
 * and the class has its own fresh container, so only the rows a test saves are in the table.
 */
@DataJpaTest
// Spring caches contexts by configuration, and the datasource URL is bound to this class's own
// container. Without a property that differs from TaskRepositoryAdapterTest, whichever of the two
// runs second would reuse the first one's context and talk to its already-stopped container.
@TestPropertySource(properties = "spring.jpa.open-in-view=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryAdapterSearchTest extends AbstractPostgresIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    private TaskJpaRepository taskJpaRepository;

    private TaskRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TaskRepositoryAdapter(taskJpaRepository);
    }

    @Nested
    class Filtering {

        private UUID todoMarch;
        private UUID todoJune;
        private UUID inProgressJune;
        private UUID doneSeptember;
        private UUID todoNoDueDate;

        @BeforeEach
        void saveFixtures() {
            todoMarch = save(id(1), "A", TaskStatus.TODO, LocalDate.of(2026, 3, 1), 1).id();
            todoJune = save(id(2), "B", TaskStatus.TODO, LocalDate.of(2026, 6, 15), 2).id();
            inProgressJune = save(id(3), "C", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 6, 15), 3).id();
            doneSeptember = save(id(4), "D", TaskStatus.DONE, LocalDate.of(2026, 9, 1), 4).id();
            todoNoDueDate = save(id(5), "E", TaskStatus.TODO, null, 5).id();
        }

        @Test
        void filtersByStatusOnly() {
            // Arrange
            TaskQuery query = query(TaskStatus.TODO, null, null);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(todoMarch, todoJune, todoNoDueDate);
            assertThat(result.totalElements()).isEqualTo(3);
        }

        @Test
        void filtersByDueAfterOnlyAndExcludesTheBoundary() {
            // Arrange
            TaskQuery query = query(null, LocalDate.of(2026, 6, 15), null);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(doneSeptember);
        }

        @Test
        void filtersByDueBeforeOnlyAndExcludesTheBoundary() {
            // Arrange
            TaskQuery query = query(null, null, LocalDate.of(2026, 6, 15));

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(todoMarch);
        }

        @Test
        void filtersByBothDueBoundsExclusively() {
            // Arrange
            TaskQuery query = query(null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 1));

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(todoJune, inProgressJune);
        }

        @Test
        void combinesStatusWithDueDateBounds() {
            // Arrange
            TaskQuery query = query(TaskStatus.TODO, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(todoMarch, todoJune);
        }

        @Test
        void neverMatchesATaskWithoutDueDateWhenOnlyDueAfterIsGiven() {
            // Arrange
            TaskQuery query = query(null, LocalDate.of(2000, 1, 1), null);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).doesNotContain(todoNoDueDate);
            assertThat(result.totalElements()).isEqualTo(4);
        }

        @Test
        void neverMatchesATaskWithoutDueDateWhenOnlyDueBeforeIsGiven() {
            // Arrange
            TaskQuery query = query(null, null, LocalDate.of(2100, 1, 1));

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).doesNotContain(todoNoDueDate);
            assertThat(result.totalElements()).isEqualTo(4);
        }

        @Test
        void returnsEveryTaskWhenNoFilterIsGiven() {
            // Arrange
            TaskQuery query = query(null, null, null);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result))
                    .containsExactly(todoMarch, todoJune, inProgressJune, doneSeptember, todoNoDueDate);
            assertThat(result.totalElements()).isEqualTo(5);
        }

        @Test
        void returnsNothingWhenNoTaskMatches() {
            // Arrange
            TaskQuery query = query(TaskStatus.DONE, LocalDate.of(2026, 9, 1), null);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }

    @Nested
    class Sorting {

        // Bravo is created first and due last, Alpha is created second and due first, and so on, so
        // that every field yields a different order.
        private UUID bravo;
        private UUID alpha;
        private UUID charlie;

        @BeforeEach
        void saveFixtures() {
            bravo = save(id(1), "Bravo", TaskStatus.TODO, LocalDate.of(2026, 5, 1), 1).id();
            alpha = save(id(2), "Alpha", TaskStatus.DONE, LocalDate.of(2026, 3, 1), 2).id();
            charlie = save(id(3), "Charlie", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 4, 1), 3).id();
        }

        @Test
        void sortsByCreatedAtAscending() {
            // Arrange
            TaskQuery query = sorted(TaskSortField.CREATED_AT, SortDirection.ASC);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(bravo, alpha, charlie);
        }

        @Test
        void sortsByCreatedAtDescending() {
            // Arrange
            TaskQuery query = sorted(TaskSortField.CREATED_AT, SortDirection.DESC);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(charlie, alpha, bravo);
        }

        @Test
        void sortsByDueDateAscending() {
            // Arrange
            TaskQuery query = sorted(TaskSortField.DUE_DATE, SortDirection.ASC);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(alpha, charlie, bravo);
        }

        @Test
        void sortsByDueDateDescending() {
            // Arrange
            TaskQuery query = sorted(TaskSortField.DUE_DATE, SortDirection.DESC);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(bravo, charlie, alpha);
        }

        @Test
        void sortsByTitleAscending() {
            // Arrange
            TaskQuery query = sorted(TaskSortField.TITLE, SortDirection.ASC);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(alpha, bravo, charlie);
        }

        @Test
        void sortsByTitleDescending() {
            // Arrange
            TaskQuery query = sorted(TaskSortField.TITLE, SortDirection.DESC);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(charlie, bravo, alpha);
        }

        /**
         * Documents current behaviour rather than a feature: status is stored as its name, so
         * ascending order is alphabetical (DONE, IN_PROGRESS, TODO), not the TODO, IN_PROGRESS, DONE
         * lifecycle order.
         */
        @Test
        void sortsStatusAlphabeticallyByNameNotByLifecycleOrder() {
            // Arrange
            TaskQuery query = sorted(TaskSortField.STATUS, SortDirection.ASC);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(alpha, charlie, bravo);
            assertThat(result.content()).extracting(Task::status)
                    .containsExactly(TaskStatus.DONE, TaskStatus.IN_PROGRESS, TaskStatus.TODO);
        }

        @Test
        void sortsStatusDescendingInReverseAlphabeticalOrder() {
            // Arrange
            TaskQuery query = sorted(TaskSortField.STATUS, SortDirection.DESC);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(bravo, charlie, alpha);
        }

        @Test
        void putsNullDueDatesLastWhenSortingAscending() {
            // Arrange
            UUID undatedOne = save(id(4), "Delta", TaskStatus.TODO, null, 4).id();
            UUID undatedTwo = save(id(5), "Echo", TaskStatus.TODO, null, 5).id();
            TaskQuery query = sorted(TaskSortField.DUE_DATE, SortDirection.ASC);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(alpha, charlie, bravo, undatedOne, undatedTwo);
        }

        @Test
        void putsNullDueDatesLastWhenSortingDescending() {
            // Arrange
            UUID undatedOne = save(id(4), "Delta", TaskStatus.TODO, null, 4).id();
            UUID undatedTwo = save(id(5), "Echo", TaskStatus.TODO, null, 5).id();
            TaskQuery query = sorted(TaskSortField.DUE_DATE, SortDirection.DESC);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(bravo, charlie, alpha, undatedOne, undatedTwo);
        }

        @Test
        void breaksTiesByIdAscendingWhenSortingAscending() {
            // Arrange
            UUID third = save(id(13), "Same", TaskStatus.TODO, null, 6).id();
            UUID first = save(id(11), "Same", TaskStatus.TODO, null, 7).id();
            UUID second = save(id(12), "Same", TaskStatus.TODO, null, 8).id();
            TaskQuery query = new TaskQuery(null, null, null, new PageRequest(0, 100),
                    new TaskSort(TaskSortField.TITLE, SortDirection.ASC));

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(alpha, bravo, charlie, first, second, third);
        }

        @Test
        void breaksTiesByIdAscendingEvenWhenSortingDescending() {
            // Arrange
            UUID third = save(id(13), "Same", TaskStatus.TODO, null, 6).id();
            UUID first = save(id(11), "Same", TaskStatus.TODO, null, 7).id();
            UUID second = save(id(12), "Same", TaskStatus.TODO, null, 8).id();
            TaskQuery query = new TaskQuery(null, null, null, new PageRequest(0, 100),
                    new TaskSort(TaskSortField.TITLE, SortDirection.DESC));

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(first, second, third, charlie, bravo, alpha);
        }
    }

    @Nested
    class Paging {

        private UUID first;
        private UUID second;
        private UUID third;
        private UUID fourth;
        private UUID fifth;

        @BeforeEach
        void saveFixtures() {
            first = save(id(1), "Task 1", TaskStatus.TODO, null, 1).id();
            second = save(id(2), "Task 2", TaskStatus.TODO, null, 2).id();
            third = save(id(3), "Task 3", TaskStatus.TODO, null, 3).id();
            fourth = save(id(4), "Task 4", TaskStatus.TODO, null, 4).id();
            fifth = save(id(5), "Task 5", TaskStatus.TODO, null, 5).id();
        }

        @Test
        void returnsTheFirstPage() {
            // Arrange
            TaskQuery query = paged(0, 2);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(first, second);
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(2);
            assertThat(result.totalElements()).isEqualTo(5);
            assertThat(result.totalPages()).isEqualTo(3);
        }

        @Test
        void returnsAMiddlePage() {
            // Arrange
            TaskQuery query = paged(1, 2);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(third, fourth);
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(2);
            assertThat(result.totalElements()).isEqualTo(5);
            assertThat(result.totalPages()).isEqualTo(3);
        }

        @Test
        void returnsAShorterLastPage() {
            // Arrange
            TaskQuery query = paged(2, 2);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(fifth);
            assertThat(result.page()).isEqualTo(2);
            assertThat(result.totalElements()).isEqualTo(5);
            assertThat(result.totalPages()).isEqualTo(3);
        }

        @Test
        void returnsEmptyContentWithCorrectTotalsForAPageBeyondTheLastOne() {
            // Arrange
            TaskQuery query = paged(7, 2);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(result.content()).isEmpty();
            assertThat(result.page()).isEqualTo(7);
            assertThat(result.size()).isEqualTo(2);
            assertThat(result.totalElements()).isEqualTo(5);
            assertThat(result.totalPages()).isEqualTo(3);
        }

        @Test
        void returnsEverythingOnOnePageWhenTheSizeExceedsTheData() {
            // Arrange
            TaskQuery query = paged(0, 100);

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(first, second, third, fourth, fifth);
            assertThat(result.size()).isEqualTo(100);
            assertThat(result.totalElements()).isEqualTo(5);
            assertThat(result.totalPages()).isEqualTo(1);
        }

        @Test
        void countsOnlyTheMatchingTasksInTheTotals() {
            // Arrange
            save(id(6), "Task 6", TaskStatus.DONE, null, 6);
            TaskQuery query = new TaskQuery(TaskStatus.TODO, null, null, new PageRequest(1, 2),
                    new TaskSort(TaskSortField.TITLE, SortDirection.ASC));

            // Act
            Page<Task> result = adapter.search(query);

            // Assert
            assertThat(ids(result)).containsExactly(third, fourth);
            assertThat(result.totalElements()).isEqualTo(5);
        }
    }

    private Task save(UUID id, String title, TaskStatus status, LocalDate dueDate, int createdAtOffsetMinutes) {
        Instant createdAt = T0.plusSeconds(60L * createdAtOffsetMinutes);
        return adapter.save(Task.reconstruct(id, title, null, status, dueDate, createdAt, createdAt));
    }

    /** A deterministic id that sorts in the same order as {@code n}. */
    private static UUID id(long n) {
        return new UUID(0L, n);
    }

    /** Filter-only query, sorted by title ascending so the expected order follows the fixtures. */
    private static TaskQuery query(TaskStatus status, LocalDate dueAfter, LocalDate dueBefore) {
        return new TaskQuery(status, dueAfter, dueBefore, new PageRequest(0, 100),
                new TaskSort(TaskSortField.TITLE, SortDirection.ASC));
    }

    private static TaskQuery sorted(TaskSortField field, SortDirection direction) {
        return new TaskQuery(null, null, null, new PageRequest(0, 100), new TaskSort(field, direction));
    }

    private static TaskQuery paged(int page, int size) {
        return new TaskQuery(null, null, null, new PageRequest(page, size),
                new TaskSort(TaskSortField.TITLE, SortDirection.ASC));
    }

    private static List<UUID> ids(Page<Task> page) {
        return page.content().stream().map(Task::id).toList();
    }
}

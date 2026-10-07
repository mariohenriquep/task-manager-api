package com.taskmanager.api.infrastructure.persistence;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.model.TaskStatus;
import com.taskmanager.api.infrastructure.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryAdapterTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TaskJpaRepository taskJpaRepository;

    private TaskRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TaskRepositoryAdapter(taskJpaRepository);
    }

    @Test
    void savesAndRetrievesATaskById() {
        // Arrange
        Task task = Task.create("Buy groceries", "Milk, eggs, bread", LocalDate.of(2026, 9, 1), Clock.systemUTC());

        // Act
        adapter.save(task);
        Optional<Task> found = adapter.findById(task.id());

        // Assert
        assertThat(found).isPresent();
        assertRoundTripsCorrectly(task, found.get());
    }

    @Test
    void returnsEmptyWhenTaskDoesNotExist() {
        // Arrange
        UUID unknownId = UUID.randomUUID();

        // Act
        Optional<Task> found = adapter.findById(unknownId);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void findsAllPersistedTasks() {
        // Arrange
        Task first = Task.create("First", null, null, Clock.systemUTC());
        Task second = Task.create("Second", null, null, Clock.systemUTC());
        adapter.save(first);
        adapter.save(second);

        // Act
        List<Task> all = adapter.findAll();

        // Assert
        assertThat(all).extracting(Task::id).containsExactlyInAnyOrder(first.id(), second.id());
    }

    @Test
    void updatesAnExistingTaskOnSave() {
        // Arrange
        Task task = Task.create("Original", null, null, Clock.systemUTC());
        adapter.save(task);
        Task started = task.start(Clock.systemUTC());

        // Act
        adapter.save(started);
        Optional<Task> found = adapter.findById(task.id());
        List<Task> all = adapter.findAll();

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(all).hasSize(1);
    }

    @Test
    void deletesATask() {
        // Arrange
        Task task = Task.create("To delete", null, null, Clock.systemUTC());
        adapter.save(task);

        // Act
        adapter.deleteById(task.id());
        Optional<Task> found = adapter.findById(task.id());

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void reportsWhetherATaskExists() {
        // Arrange
        Task task = Task.create("Existing", null, null, Clock.systemUTC());
        adapter.save(task);

        // Act
        boolean existingTaskExists = adapter.existsById(task.id());
        boolean unknownTaskExists = adapter.existsById(UUID.randomUUID());

        // Assert
        assertThat(existingTaskExists).isTrue();
        assertThat(unknownTaskExists).isFalse();
    }

    private void assertRoundTripsCorrectly(Task expected, Task actual) {
        assertThat(actual.id()).isEqualTo(expected.id());
        assertThat(actual.title()).isEqualTo(expected.title());
        assertThat(actual.description()).isEqualTo(expected.description());
        assertThat(actual.status()).isEqualTo(expected.status());
        assertThat(actual.dueDate()).isEqualTo(expected.dueDate());
        assertThat(actual.createdAt().truncatedTo(ChronoUnit.MILLIS))
                .isEqualTo(expected.createdAt().truncatedTo(ChronoUnit.MILLIS));
        assertThat(actual.updatedAt().truncatedTo(ChronoUnit.MILLIS))
                .isEqualTo(expected.updatedAt().truncatedTo(ChronoUnit.MILLIS));
    }
}

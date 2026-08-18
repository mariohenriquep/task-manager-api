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
        Task task = Task.create("Buy groceries", "Milk, eggs, bread", LocalDate.of(2026, 9, 1), Clock.systemUTC());

        adapter.save(task);
        Optional<Task> found = adapter.findById(task.id());

        assertThat(found).isPresent();
        assertRoundTripsCorrectly(task, found.get());
    }

    @Test
    void returnsEmptyWhenTaskDoesNotExist() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findsAllPersistedTasks() {
        Task first = Task.create("First", null, null, Clock.systemUTC());
        Task second = Task.create("Second", null, null, Clock.systemUTC());
        adapter.save(first);
        adapter.save(second);

        List<Task> all = adapter.findAll();

        assertThat(all).extracting(Task::id).containsExactlyInAnyOrder(first.id(), second.id());
    }

    @Test
    void updatesAnExistingTaskOnSave() {
        Task task = Task.create("Original", null, null, Clock.systemUTC());
        adapter.save(task);

        Task started = task.start(Clock.systemUTC());
        adapter.save(started);

        Optional<Task> found = adapter.findById(task.id());
        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(adapter.findAll()).hasSize(1);
    }

    @Test
    void deletesATask() {
        Task task = Task.create("To delete", null, null, Clock.systemUTC());
        adapter.save(task);

        adapter.deleteById(task.id());

        assertThat(adapter.findById(task.id())).isEmpty();
    }

    @Test
    void reportsWhetherATaskExists() {
        Task task = Task.create("Existing", null, null, Clock.systemUTC());
        adapter.save(task);

        assertThat(adapter.existsById(task.id())).isTrue();
        assertThat(adapter.existsById(UUID.randomUUID())).isFalse();
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

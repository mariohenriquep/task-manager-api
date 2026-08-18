package com.taskmanager.api.application.usecase;

import com.taskmanager.api.application.command.UpdateTaskCommand;
import com.taskmanager.api.domain.exception.TaskNotFoundException;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTaskServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TaskRepository taskRepository;

    private UpdateTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateTaskService(taskRepository, FIXED_CLOCK);
    }

    @Test
    void updatesExistingTaskDetails() {
        Task existing = Task.create("Original", "Original desc", null, FIXED_CLOCK);
        when(taskRepository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = useCase.execute(new UpdateTaskCommand(existing.id(), "New title", "New desc", LocalDate.of(2026, 10, 1)));

        assertThat(result.title()).isEqualTo("New title");
        assertThat(result.description()).isEqualTo("New desc");
        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void throwsWhenTaskDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateTaskCommand(id, "Title", null, null)))
                .isInstanceOf(TaskNotFoundException.class);
    }
}

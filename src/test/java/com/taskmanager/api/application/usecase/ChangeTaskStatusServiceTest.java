package com.taskmanager.api.application.usecase;

import com.taskmanager.api.application.command.ChangeTaskStatusCommand;
import com.taskmanager.api.application.command.TaskStatusAction;
import com.taskmanager.api.domain.exception.InvalidTaskStatusTransitionException;
import com.taskmanager.api.domain.exception.TaskNotFoundException;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.model.TaskStatus;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeTaskStatusServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TaskRepository taskRepository;

    private ChangeTaskStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangeTaskStatusService(taskRepository, FIXED_CLOCK);
    }

    @Test
    void startsATodoTask() {
        Task task = Task.create("Task", null, null, FIXED_CLOCK);
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = useCase.execute(new ChangeTaskStatusCommand(task.id(), TaskStatusAction.START));

        assertThat(result.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void completesATask() {
        Task task = Task.create("Task", null, null, FIXED_CLOCK);
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = useCase.execute(new ChangeTaskStatusCommand(task.id(), TaskStatusAction.COMPLETE));

        assertThat(result.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void reopensADoneTask() {
        Task task = Task.create("Task", null, null, FIXED_CLOCK).complete(FIXED_CLOCK);
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = useCase.execute(new ChangeTaskStatusCommand(task.id(), TaskStatusAction.REOPEN));

        assertThat(result.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void propagatesInvalidTransition() {
        Task task = Task.create("Task", null, null, FIXED_CLOCK).complete(FIXED_CLOCK);
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> useCase.execute(new ChangeTaskStatusCommand(task.id(), TaskStatusAction.START)))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);
    }

    @Test
    void throwsWhenTaskDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ChangeTaskStatusCommand(id, TaskStatusAction.START)))
                .isInstanceOf(TaskNotFoundException.class);
    }
}

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
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        // Arrange
        Task task = Task.create("Task", null, null, FIXED_CLOCK);
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ChangeTaskStatusCommand command = new ChangeTaskStatusCommand(task.id(), TaskStatusAction.START);

        // Act
        Task result = useCase.execute(command);

        // Assert
        assertThat(result.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void completesATask() {
        // Arrange
        Task task = Task.create("Task", null, null, FIXED_CLOCK);
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ChangeTaskStatusCommand command = new ChangeTaskStatusCommand(task.id(), TaskStatusAction.COMPLETE);

        // Act
        Task result = useCase.execute(command);

        // Assert
        assertThat(result.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void reopensADoneTask() {
        // Arrange
        Task task = Task.create("Task", null, null, FIXED_CLOCK).complete(FIXED_CLOCK);
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ChangeTaskStatusCommand command = new ChangeTaskStatusCommand(task.id(), TaskStatusAction.REOPEN);

        // Act
        Task result = useCase.execute(command);

        // Assert
        assertThat(result.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void propagatesInvalidTransition() {
        // Arrange
        Task task = Task.create("Task", null, null, FIXED_CLOCK).complete(FIXED_CLOCK);
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));
        Executable act = () -> useCase.execute(new ChangeTaskStatusCommand(task.id(), TaskStatusAction.START));

        // Act
        InvalidTaskStatusTransitionException ex = assertThrows(InvalidTaskStatusTransitionException.class, act);

        // Assert
        assertThat(ex.getMessage()).isEqualTo("Cannot move task from status DONE to IN_PROGRESS");
        verify(taskRepository, never()).save(any(Task.class));
    }

    static Stream<Arguments> illegalTransitions() {
        return Stream.of(
                Arguments.of(TaskStatus.IN_PROGRESS, TaskStatusAction.START, TaskStatus.IN_PROGRESS),
                Arguments.of(TaskStatus.DONE, TaskStatusAction.START, TaskStatus.IN_PROGRESS),
                Arguments.of(TaskStatus.DONE, TaskStatusAction.COMPLETE, TaskStatus.DONE),
                Arguments.of(TaskStatus.TODO, TaskStatusAction.REOPEN, TaskStatus.TODO),
                Arguments.of(TaskStatus.IN_PROGRESS, TaskStatusAction.REOPEN, TaskStatus.TODO));
    }

    @ParameterizedTest(name = "{1} from {0} is rejected")
    @MethodSource("illegalTransitions")
    void propagatesInvalidTransitionAndDoesNotSave(TaskStatus current, TaskStatusAction action, TaskStatus target) {
        // Arrange
        Task task = taskInStatus(current);
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));
        Executable act = () -> useCase.execute(new ChangeTaskStatusCommand(task.id(), action));

        // Act
        InvalidTaskStatusTransitionException ex = assertThrows(InvalidTaskStatusTransitionException.class, act);

        // Assert
        assertThat(ex.getMessage()).isEqualTo("Cannot move task from status " + current + " to " + target);
        verify(taskRepository, never()).save(any(Task.class));
    }

    private static Task taskInStatus(TaskStatus status) {
        Task task = Task.create("Task", null, null, FIXED_CLOCK);
        return switch (status) {
            case TODO -> task;
            case IN_PROGRESS -> task.start(FIXED_CLOCK);
            case DONE -> task.complete(FIXED_CLOCK);
        };
    }

    @Test
    void throwsWhenTaskDoesNotExist() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());
        Executable act = () -> useCase.execute(new ChangeTaskStatusCommand(id, TaskStatusAction.START));

        // Act
        TaskNotFoundException ex = assertThrows(TaskNotFoundException.class, act);

        // Assert
        assertThat(ex.getMessage()).isEqualTo("Task not found: " + id);
        verify(taskRepository, never()).save(any(Task.class));
    }
}

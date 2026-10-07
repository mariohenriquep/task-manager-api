package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.exception.TaskNotFoundException;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private GetTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTaskService(taskRepository);
    }

    @Test
    void returnsTaskWhenFound() {
        // Arrange
        Task task = Task.create("Task", null, null, Clock.systemUTC());
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));

        // Act
        Task result = useCase.execute(task.id());

        // Assert
        assertThat(result).isEqualTo(task);
    }

    @Test
    void throwsWhenTaskDoesNotExist() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());
        Executable act = () -> useCase.execute(id);

        // Act
        TaskNotFoundException ex = assertThrows(TaskNotFoundException.class, act);

        // Assert
        assertThat(ex.getMessage()).isEqualTo("Task not found: " + id);
        verifyNoMoreInteractions(taskRepository);
    }
}

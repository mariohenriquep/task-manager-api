package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.exception.TaskNotFoundException;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteTaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private DeleteTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteTaskService(taskRepository);
    }

    @Test
    void deletesExistingTask() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(taskRepository.existsById(id)).thenReturn(true);

        // Act
        useCase.execute(id);

        // Assert
        verify(taskRepository).deleteById(id);
    }

    @Test
    void throwsWhenTaskDoesNotExist() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(taskRepository.existsById(id)).thenReturn(false);
        Executable act = () -> useCase.execute(id);

        // Act
        TaskNotFoundException ex = assertThrows(TaskNotFoundException.class, act);

        // Assert
        assertThat(ex.getMessage()).isEqualTo("Task not found: " + id);
        verify(taskRepository, never()).deleteById(id);
    }
}

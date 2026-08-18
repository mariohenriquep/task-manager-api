package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.exception.TaskNotFoundException;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        UUID id = UUID.randomUUID();
        when(taskRepository.existsById(id)).thenReturn(true);

        useCase.execute(id);

        verify(taskRepository).deleteById(id);
    }

    @Test
    void throwsWhenTaskDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(taskRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(TaskNotFoundException.class);
    }
}

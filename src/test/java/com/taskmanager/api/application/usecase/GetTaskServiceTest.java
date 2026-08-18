package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.exception.TaskNotFoundException;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        Task task = Task.create("Task", null, null, Clock.systemUTC());
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));

        Task result = useCase.execute(task.id());

        assertThat(result).isEqualTo(task);
    }

    @Test
    void throwsWhenTaskDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(TaskNotFoundException.class);
    }
}

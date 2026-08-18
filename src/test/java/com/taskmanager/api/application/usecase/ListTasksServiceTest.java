package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTasksServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private ListTasksUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListTasksService(taskRepository);
    }

    @Test
    void returnsAllTasksFromRepository() {
        Task first = Task.create("First", null, null, Clock.systemUTC());
        Task second = Task.create("Second", null, null, Clock.systemUTC());
        when(taskRepository.findAll()).thenReturn(List.of(first, second));

        List<Task> result = useCase.execute();

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void returnsEmptyListWhenNoTasksExist() {
        when(taskRepository.findAll()).thenReturn(List.of());

        assertThat(useCase.execute()).isEmpty();
    }
}

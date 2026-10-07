package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.model.TaskStatus;
import com.taskmanager.api.domain.query.Page;
import com.taskmanager.api.domain.query.PageRequest;
import com.taskmanager.api.domain.query.SortDirection;
import com.taskmanager.api.domain.query.TaskQuery;
import com.taskmanager.api.domain.query.TaskSort;
import com.taskmanager.api.domain.query.TaskSortField;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchTasksServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private SearchTasksUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchTasksService(taskRepository);
    }

    @Test
    void returnsExactlyWhatTheRepositoryReturns() {
        // Arrange
        TaskQuery query = TaskQuery.all();
        Task first = Task.create("First", null, null, Clock.systemUTC());
        Task second = Task.create("Second", null, null, Clock.systemUTC());
        Page<Task> expected = new Page<>(List.of(first, second), 0, 20, 2);
        when(taskRepository.search(query)).thenReturn(expected);

        // Act
        Page<Task> result = useCase.execute(query);

        // Assert
        assertThat(result).isSameAs(expected);
    }

    @Test
    void returnsAnEmptyPageWhenNothingMatches() {
        // Arrange
        TaskQuery query = TaskQuery.all();
        when(taskRepository.search(query)).thenReturn(new Page<>(List.of(), 0, 20, 0));

        // Act
        Page<Task> result = useCase.execute(query);

        // Assert
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void passesTheQueryThroughUnchanged() {
        // Arrange
        TaskQuery query = new TaskQuery(
                TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                new PageRequest(2, 5),
                new TaskSort(TaskSortField.DUE_DATE, SortDirection.ASC));
        when(taskRepository.search(query)).thenReturn(new Page<>(List.of(), 2, 5, 0));
        ArgumentCaptor<TaskQuery> captor = ArgumentCaptor.forClass(TaskQuery.class);

        // Act
        useCase.execute(query);

        // Assert
        verify(taskRepository).search(captor.capture());
        assertThat(captor.getValue()).isSameAs(query);
    }
}

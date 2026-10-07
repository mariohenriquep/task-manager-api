package com.taskmanager.api.infrastructure.web.mapper;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.query.Page;
import com.taskmanager.api.infrastructure.web.dto.PageResponse;
import com.taskmanager.api.infrastructure.web.dto.TaskResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskWebMapperTest {

    private static final Clock CLOCK = Clock.systemUTC();

    @Test
    void mapsAPageToAnEnvelopeOfTaskResponses() {
        // Arrange
        Task first = Task.create("First", null, null, CLOCK);
        Task second = Task.create("Second", "desc", null, CLOCK);
        Page<Task> page = new Page<>(List.of(first, second), 1, 2, 5);

        // Act
        PageResponse<TaskResponse> response = TaskWebMapper.toPageResponse(page);

        // Assert
        assertThat(response.content()).containsExactly(
                TaskWebMapper.toResponse(first), TaskWebMapper.toResponse(second));
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void mapsAnEmptyPageToAnEmptyEnvelope() {
        // Arrange
        Page<Task> page = new Page<>(List.of(), 0, 20, 0);

        // Act
        PageResponse<TaskResponse> response = TaskWebMapper.toPageResponse(page);

        // Assert
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }
}

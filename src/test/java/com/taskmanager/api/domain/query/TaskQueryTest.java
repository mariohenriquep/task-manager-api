package com.taskmanager.api.domain.query;

import com.taskmanager.api.domain.exception.InvalidTaskQueryException;
import com.taskmanager.api.domain.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskQueryTest {

    private static final LocalDate JUNE_1 = LocalDate.of(2026, 6, 1);
    private static final LocalDate JUNE_2 = LocalDate.of(2026, 6, 2);

    @Test
    void allHasNoFiltersAndDefaultPageAndSort() {
        // Arrange
        // (no inputs: all() takes none)

        // Act
        TaskQuery query = TaskQuery.all();

        // Assert
        assertThat(query.status()).isNull();
        assertThat(query.dueAfter()).isNull();
        assertThat(query.dueBefore()).isNull();
        assertThat(query.pageRequest()).isEqualTo(PageRequest.defaults());
        assertThat(query.sort()).isEqualTo(TaskSort.defaults());
    }

    @Test
    void acceptsAllFiltersTogether() {
        // Arrange
        PageRequest pageRequest = new PageRequest(2, 50);
        TaskSort sort = new TaskSort(TaskSortField.TITLE, SortDirection.ASC);

        // Act
        TaskQuery query = new TaskQuery(TaskStatus.IN_PROGRESS, JUNE_1, JUNE_2, pageRequest, sort);

        // Assert
        assertThat(query.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(query.dueAfter()).isEqualTo(JUNE_1);
        assertThat(query.dueBefore()).isEqualTo(JUNE_2);
        assertThat(query.pageRequest()).isEqualTo(pageRequest);
        assertThat(query.sort()).isEqualTo(sort);
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void acceptsAnyStatusFilterOnItsOwn(TaskStatus status) {
        // Arrange
        PageRequest pageRequest = PageRequest.defaults();

        // Act
        TaskQuery query = new TaskQuery(status, null, null, pageRequest, TaskSort.defaults());

        // Assert
        assertThat(query.status()).isEqualTo(status);
    }

    @Test
    void acceptsOnlyDueAfter() {
        // Arrange
        LocalDate dueAfter = JUNE_1;

        // Act
        TaskQuery query = new TaskQuery(null, dueAfter, null, PageRequest.defaults(), TaskSort.defaults());

        // Assert
        assertThat(query.dueAfter()).isEqualTo(JUNE_1);
        assertThat(query.dueBefore()).isNull();
    }

    @Test
    void acceptsOnlyDueBefore() {
        // Arrange
        LocalDate dueBefore = JUNE_1;

        // Act
        TaskQuery query = new TaskQuery(null, null, dueBefore, PageRequest.defaults(), TaskSort.defaults());

        // Assert
        assertThat(query.dueAfter()).isNull();
        assertThat(query.dueBefore()).isEqualTo(JUNE_1);
    }

    @Test
    void acceptsDueAfterOneDayBeforeDueBefore() {
        // Arrange
        Executable act = () -> new TaskQuery(null, JUNE_1, JUNE_2, PageRequest.defaults(), TaskSort.defaults());

        // Act
        assertDoesNotThrow(act);

        // Assert
        assertThat(JUNE_1.plusDays(1)).isEqualTo(JUNE_2);
    }

    @Test
    void rejectsDueAfterEqualToDueBefore() {
        // Arrange
        Executable act = () -> new TaskQuery(null, JUNE_1, JUNE_1, PageRequest.defaults(), TaskSort.defaults());

        // Act
        InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

        // Assert
        assertThat(ex.getMessage()).contains("dueAfter").contains("before").contains("dueBefore");
    }

    @Test
    void rejectsDueAfterLaterThanDueBefore() {
        // Arrange
        Executable act = () -> new TaskQuery(null, JUNE_2, JUNE_1, PageRequest.defaults(), TaskSort.defaults());

        // Act
        InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

        // Assert
        assertThat(ex.getMessage()).contains("dueAfter").contains("before").contains("dueBefore");
    }

    @Test
    void rejectsNullPageRequest() {
        // Arrange
        Executable act = () -> new TaskQuery(null, null, null, null, TaskSort.defaults());

        // Act
        NullPointerException ex = assertThrows(NullPointerException.class, act);

        // Assert
        assertThat(ex.getMessage()).contains("pageRequest");
    }

    @Test
    void rejectsNullSort() {
        // Arrange
        Executable act = () -> new TaskQuery(null, null, null, PageRequest.defaults(), null);

        // Act
        NullPointerException ex = assertThrows(NullPointerException.class, act);

        // Assert
        assertThat(ex.getMessage()).contains("sort");
    }
}

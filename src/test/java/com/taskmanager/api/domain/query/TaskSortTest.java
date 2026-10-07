package com.taskmanager.api.domain.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskSortTest {

    @Test
    void defaultsAreCreatedAtDescending() {
        // Arrange
        // (no inputs: defaults() takes none)

        // Act
        TaskSort defaults = TaskSort.defaults();

        // Assert
        assertThat(defaults.field()).isEqualTo(TaskSortField.CREATED_AT);
        assertThat(defaults.direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void holdsGivenFieldAndDirection() {
        // Arrange
        TaskSortField field = TaskSortField.DUE_DATE;
        SortDirection direction = SortDirection.ASC;

        // Act
        TaskSort sort = new TaskSort(field, direction);

        // Assert
        assertThat(sort.field()).isEqualTo(TaskSortField.DUE_DATE);
        assertThat(sort.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void rejectsNullField() {
        // Arrange
        Executable act = () -> new TaskSort(null, SortDirection.ASC);

        // Act
        NullPointerException ex = assertThrows(NullPointerException.class, act);

        // Assert
        assertThat(ex.getMessage()).contains("field");
    }

    @Test
    void rejectsNullDirection() {
        // Arrange
        Executable act = () -> new TaskSort(TaskSortField.TITLE, null);

        // Act
        NullPointerException ex = assertThrows(NullPointerException.class, act);

        // Assert
        assertThat(ex.getMessage()).contains("direction");
    }

    @Test
    void sortFieldsAreTheSupportedSet() {
        // Arrange
        // (enum values are read directly)

        // Act
        TaskSortField[] fields = TaskSortField.values();

        // Assert
        assertThat(fields).containsExactly(
                TaskSortField.CREATED_AT, TaskSortField.DUE_DATE, TaskSortField.TITLE, TaskSortField.STATUS);
    }

    @Test
    void sortDirectionsAreAscendingAndDescending() {
        // Arrange
        // (enum values are read directly)

        // Act
        SortDirection[] directions = SortDirection.values();

        // Assert
        assertThat(directions).containsExactly(SortDirection.ASC, SortDirection.DESC);
    }
}

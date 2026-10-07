package com.taskmanager.api.domain.query;

import java.util.Objects;

/**
 * How to order a task result set.
 *
 * @param field     the attribute to order by, never null
 * @param direction ascending or descending, never null
 */
public record TaskSort(TaskSortField field, SortDirection direction) {

    public TaskSort {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
    }

    /** Newest tasks first: {@link TaskSortField#CREATED_AT} descending. */
    public static TaskSort defaults() {
        return new TaskSort(TaskSortField.CREATED_AT, SortDirection.DESC);
    }
}

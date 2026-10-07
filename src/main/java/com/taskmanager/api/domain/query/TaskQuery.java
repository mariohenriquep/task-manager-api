package com.taskmanager.api.domain.query;

import com.taskmanager.api.domain.exception.InvalidTaskQueryException;
import com.taskmanager.api.domain.model.TaskStatus;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A request for a page of tasks: optional filters, plus how to order and slice the result.
 *
 * <p>Both due-date bounds are <em>exclusive</em>: a task matches when
 * {@code dueAfter < dueDate < dueBefore}. A task with no due date never matches a due-date bound.
 * Every filter is optional ({@code null} means "do not filter on this").
 *
 * @param status      only tasks in this status, or {@code null} for any
 * @param dueAfter    only tasks due strictly after this date, or {@code null} for no lower bound
 * @param dueBefore   only tasks due strictly before this date, or {@code null} for no upper bound
 * @param pageRequest which page to return, never null
 * @param sort        how to order the result, never null
 */
public record TaskQuery(
        TaskStatus status,
        LocalDate dueAfter,
        LocalDate dueBefore,
        PageRequest pageRequest,
        TaskSort sort
) {

    public TaskQuery {
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        Objects.requireNonNull(sort, "sort must not be null");
        if (dueAfter != null && dueBefore != null && !dueAfter.isBefore(dueBefore)) {
            throw new InvalidTaskQueryException("dueAfter must be before dueBefore");
        }
    }

    /** No filters, the default page and the default sort. */
    public static TaskQuery all() {
        return new TaskQuery(null, null, null, PageRequest.defaults(), TaskSort.defaults());
    }
}

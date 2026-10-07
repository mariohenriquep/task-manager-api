package com.taskmanager.api.infrastructure.web.mapper;

import com.taskmanager.api.domain.exception.InvalidTaskQueryException;
import com.taskmanager.api.domain.model.TaskStatus;
import com.taskmanager.api.domain.query.PageRequest;
import com.taskmanager.api.domain.query.SortDirection;
import com.taskmanager.api.domain.query.TaskQuery;
import com.taskmanager.api.domain.query.TaskSort;
import com.taskmanager.api.domain.query.TaskSortField;

import java.time.LocalDate;

/**
 * Builds the domain {@link TaskQuery} from the raw request parameters of {@code GET /api/tasks}.
 *
 * <p>The only thing parsed here is the {@code sort} wire syntax ({@code field[,direction]}); page,
 * size and due-date-window rules live in the domain and surface as {@link InvalidTaskQueryException}
 * from the {@code PageRequest} and {@code TaskQuery} constructors.
 */
public final class TaskQueryWebMapper {

    private static final String SORT_SEPARATOR = ",";
    private static final String ALLOWED_FIELDS = "createdAt, dueDate, title, status";
    private static final String ALLOWED_DIRECTIONS = "asc, desc";

    private TaskQueryWebMapper() {
    }

    public static TaskQuery toQuery(TaskStatus status, LocalDate dueAfter, LocalDate dueBefore,
                                    String sort, int page, int size) {
        return new TaskQuery(status, dueAfter, dueBefore, new PageRequest(page, size), toSort(sort));
    }

    private static TaskSort toSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return TaskSort.defaults();
        }
        String[] parts = sort.split(SORT_SEPARATOR, -1);
        if (parts.length > 2) {
            throw new InvalidTaskQueryException(
                    "Invalid sort '" + sort + "': expected 'field,direction' or just 'field'");
        }
        TaskSortField field = toField(parts[0].trim());
        SortDirection direction = parts.length == 2 ? toDirection(parts[1].trim()) : SortDirection.ASC;
        return new TaskSort(field, direction);
    }

    private static TaskSortField toField(String field) {
        return switch (field) {
            case "createdAt" -> TaskSortField.CREATED_AT;
            case "dueDate" -> TaskSortField.DUE_DATE;
            case "title" -> TaskSortField.TITLE;
            case "status" -> TaskSortField.STATUS;
            default -> throw new InvalidTaskQueryException(
                    "Unknown sort field '" + field + "'; allowed fields: " + ALLOWED_FIELDS);
        };
    }

    private static SortDirection toDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return SortDirection.ASC;
        }
        if (direction.equalsIgnoreCase("desc")) {
            return SortDirection.DESC;
        }
        throw new InvalidTaskQueryException(
                "Unknown sort direction '" + direction + "'; allowed directions: " + ALLOWED_DIRECTIONS);
    }
}

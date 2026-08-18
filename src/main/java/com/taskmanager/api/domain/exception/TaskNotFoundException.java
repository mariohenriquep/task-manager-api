package com.taskmanager.api.domain.exception;

import java.util.UUID;

/**
 * Thrown when a {@code Task} is looked up by an identifier that has no matching record.
 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(UUID taskId) {
        super("Task not found: " + taskId);
    }
}

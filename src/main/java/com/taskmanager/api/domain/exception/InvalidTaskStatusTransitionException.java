package com.taskmanager.api.domain.exception;

import com.taskmanager.api.domain.model.TaskStatus;

/**
 * Thrown when a {@code Task} is asked to move to a status that is not reachable
 * from its current status.
 */
public class InvalidTaskStatusTransitionException extends RuntimeException {

    public InvalidTaskStatusTransitionException(TaskStatus from, TaskStatus to) {
        super("Cannot move task from status " + from + " to " + to);
    }
}

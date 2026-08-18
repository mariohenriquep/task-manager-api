package com.taskmanager.api.domain.exception;

/**
 * Thrown when a {@code Task} is asked to hold data that violates its invariants
 * (e.g. a blank title, or text exceeding the allowed length).
 */
public class InvalidTaskException extends RuntimeException {

    public InvalidTaskException(String message) {
        super(message);
    }
}

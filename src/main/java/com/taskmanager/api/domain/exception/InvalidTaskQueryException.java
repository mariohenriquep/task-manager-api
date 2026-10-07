package com.taskmanager.api.domain.exception;

/**
 * Thrown when a task query is malformed (e.g. a negative page, a page size outside the allowed
 * range, or a due-date window whose lower bound is not before its upper bound).
 */
public class InvalidTaskQueryException extends RuntimeException {

    public InvalidTaskQueryException(String message) {
        super(message);
    }
}

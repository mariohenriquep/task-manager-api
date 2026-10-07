package com.taskmanager.api.domain.query;

import com.taskmanager.api.domain.exception.InvalidTaskQueryException;

/**
 * Which slice of a result set to return: a zero-based page index and a page size.
 *
 * @param page zero-based page index, must not be negative
 * @param size number of elements per page, between 1 and {@link #MAX_SIZE} inclusive
 */
public record PageRequest(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0) {
            throw new InvalidTaskQueryException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidTaskQueryException("Page size must be between 1 and " + MAX_SIZE);
        }
    }

    /** The first page, with {@link #DEFAULT_SIZE} elements. */
    public static PageRequest defaults() {
        return new PageRequest(0, DEFAULT_SIZE);
    }
}

package com.taskmanager.api.domain.query;

import java.util.List;
import java.util.Objects;

/**
 * One page of a result set, plus enough metadata to navigate the rest.
 *
 * <p>An output type produced by a repository. It rejects values that cannot describe a real page
 * (a negative page or total, a size below 1) so {@link #totalPages()} can never divide by zero; the
 * content is defensively copied and therefore immutable. Such a value is a programming error, not a
 * bad client request, hence {@link IllegalArgumentException} rather than a query exception.
 *
 * @param content       the elements on this page
 * @param page          zero-based index of this page
 * @param size          the requested page size
 * @param totalElements how many elements match across all pages
 * @param <T>           the element type
 */
public record Page<T>(List<T> content, int page, int size, long totalElements) {

    public Page {
        Objects.requireNonNull(content, "content must not be null");
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
        content = List.copyOf(content);
    }

    /** {@code ceil(totalElements / size)}, or 0 when there are no elements. */
    public int totalPages() {
        if (totalElements == 0) {
            return 0;
        }
        long pages = (totalElements + size - 1) / size;
        return Math.toIntExact(pages);
    }
}

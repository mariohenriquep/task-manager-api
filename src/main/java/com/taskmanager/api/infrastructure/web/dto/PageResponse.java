package com.taskmanager.api.infrastructure.web.dto;

import java.util.List;

/**
 * The wire shape of one page of results: the elements plus enough metadata to navigate the rest.
 *
 * @param content       the elements on this page
 * @param page          zero-based index of this page
 * @param size          the requested page size
 * @param totalElements how many elements match across all pages
 * @param totalPages    how many pages that makes at this size (0 when nothing matches)
 * @param <T>           the element DTO type
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}

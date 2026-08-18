package com.taskmanager.api.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        String status,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {
}

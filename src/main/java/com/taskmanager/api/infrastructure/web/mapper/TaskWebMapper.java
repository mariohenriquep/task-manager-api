package com.taskmanager.api.infrastructure.web.mapper;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.infrastructure.web.dto.TaskResponse;

public final class TaskWebMapper {

    private TaskWebMapper() {
    }

    public static TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.id(),
                task.title(),
                task.description(),
                task.status().name(),
                task.dueDate(),
                task.createdAt(),
                task.updatedAt()
        );
    }
}

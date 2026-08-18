package com.taskmanager.api.infrastructure.persistence.mapper;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.infrastructure.persistence.entity.TaskJpaEntity;

/**
 * Translates between the domain {@link Task} aggregate and the {@link TaskJpaEntity}
 * persistence record. Nothing outside the persistence package should ever see a
 * {@code TaskJpaEntity} - this mapper is the boundary.
 */
public final class TaskPersistenceMapper {

    private TaskPersistenceMapper() {
    }

    public static TaskJpaEntity toEntity(Task task) {
        return new TaskJpaEntity(
                task.id(),
                task.title(),
                task.description(),
                task.status(),
                task.dueDate(),
                task.createdAt(),
                task.updatedAt()
        );
    }

    public static Task toDomain(TaskJpaEntity entity) {
        return Task.reconstruct(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getDueDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

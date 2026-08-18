package com.taskmanager.api.infrastructure.persistence;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.repository.TaskRepository;
import com.taskmanager.api.infrastructure.persistence.mapper.TaskPersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound adapter that implements the domain {@link TaskRepository} port on top of
 * Spring Data JPA. This is the only class in the codebase allowed to depend on both
 * the domain model and the JPA-specific persistence types.
 */
@Component
public class TaskRepositoryAdapter implements TaskRepository {

    private final TaskJpaRepository taskJpaRepository;

    public TaskRepositoryAdapter(TaskJpaRepository taskJpaRepository) {
        this.taskJpaRepository = taskJpaRepository;
    }

    @Override
    public Task save(Task task) {
        var saved = taskJpaRepository.save(TaskPersistenceMapper.toEntity(task));
        return TaskPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return taskJpaRepository.findById(id).map(TaskPersistenceMapper::toDomain);
    }

    @Override
    public List<Task> findAll() {
        return taskJpaRepository.findAll().stream()
                .map(TaskPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        taskJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return taskJpaRepository.existsById(id);
    }
}

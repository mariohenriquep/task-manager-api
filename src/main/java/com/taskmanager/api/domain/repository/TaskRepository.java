package com.taskmanager.api.domain.repository;

import com.taskmanager.api.domain.model.Task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port through which the domain/application layers persist and retrieve tasks.
 *
 * <p>This interface belongs to the domain because it is defined in terms the domain
 * understands (the {@link Task} aggregate); the implementation lives in the infrastructure
 * layer, which depends inward on this contract - never the other way around.
 */
public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findById(UUID id);

    List<Task> findAll();

    void deleteById(UUID id);

    boolean existsById(UUID id);
}

package com.taskmanager.api.infrastructure.persistence;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.query.Page;
import com.taskmanager.api.domain.query.SortDirection;
import com.taskmanager.api.domain.query.TaskQuery;
import com.taskmanager.api.domain.query.TaskSort;
import com.taskmanager.api.domain.query.TaskSortField;
import com.taskmanager.api.domain.repository.TaskRepository;
import com.taskmanager.api.infrastructure.persistence.entity.TaskJpaEntity;
import com.taskmanager.api.infrastructure.persistence.mapper.TaskPersistenceMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    /**
     * Translates the domain query into a Spring Data specification, sort and page request.
     *
     * <p>Due-date bounds are exclusive; a task with no due date never matches one. Ordering by
     * {@code DUE_DATE} puts tasks without a due date last in both directions, and the id ascending
     * is always the final tie-breaker so paging is stable.
     *
     * <p>Note that {@code STATUS} is persisted as its name, so ordering by it is alphabetical
     * (DONE, IN_PROGRESS, TODO), not the lifecycle order TODO, IN_PROGRESS, DONE.
     */
    @Override
    public Page<Task> search(TaskQuery query) {
        Pageable pageable = PageRequest.of(
                query.pageRequest().page(), query.pageRequest().size(), toSort(query.sort()));

        var result = taskJpaRepository.findAll(toSpecification(query), pageable);

        return new Page<>(
                result.getContent().stream().map(TaskPersistenceMapper::toDomain).toList(),
                query.pageRequest().page(),
                query.pageRequest().size(),
                result.getTotalElements());
    }

    private static Specification<TaskJpaEntity> toSpecification(TaskQuery query) {
        Specification<TaskJpaEntity> spec = Specification.unrestricted();
        if (query.status() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), query.status()));
        }
        if (query.dueAfter() != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThan(root.get("dueDate"), query.dueAfter()));
        }
        if (query.dueBefore() != null) {
            spec = spec.and((root, q, cb) -> cb.lessThan(root.get("dueDate"), query.dueBefore()));
        }
        return spec;
    }

    private static Sort toSort(TaskSort taskSort) {
        Sort.Direction direction = taskSort.direction() == SortDirection.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort.Order primary = new Sort.Order(direction, entityAttribute(taskSort));
        if (taskSort.field() == TaskSortField.DUE_DATE) {
            primary = primary.nullsLast();
        }
        return Sort.by(primary, Sort.Order.asc("id"));
    }

    /** The only place that knows which entity attribute backs each domain sort field. */
    private static String entityAttribute(TaskSort taskSort) {
        return switch (taskSort.field()) {
            case CREATED_AT -> "createdAt";
            case DUE_DATE -> "dueDate";
            case TITLE -> "title";
            case STATUS -> "status";
        };
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

package com.taskmanager.api.infrastructure.web.controller;

import com.taskmanager.api.application.command.ChangeTaskStatusCommand;
import com.taskmanager.api.application.command.CreateTaskCommand;
import com.taskmanager.api.application.command.UpdateTaskCommand;
import com.taskmanager.api.application.usecase.ChangeTaskStatusUseCase;
import com.taskmanager.api.application.usecase.CreateTaskUseCase;
import com.taskmanager.api.application.usecase.DeleteTaskUseCase;
import com.taskmanager.api.application.usecase.GetTaskUseCase;
import com.taskmanager.api.application.usecase.ListTasksUseCase;
import com.taskmanager.api.application.usecase.UpdateTaskUseCase;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.infrastructure.web.dto.ChangeTaskStatusRequest;
import com.taskmanager.api.infrastructure.web.dto.CreateTaskRequest;
import com.taskmanager.api.infrastructure.web.dto.TaskResponse;
import com.taskmanager.api.infrastructure.web.dto.UpdateTaskRequest;
import com.taskmanager.api.infrastructure.web.mapper.TaskWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Inbound HTTP adapter for the Task use cases. Depends only on the application layer's
 * use case interfaces - it has no knowledge of how tasks are persisted.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final CreateTaskUseCase createTaskUseCase;
    private final GetTaskUseCase getTaskUseCase;
    private final ListTasksUseCase listTasksUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;
    private final ChangeTaskStatusUseCase changeTaskStatusUseCase;

    public TaskController(CreateTaskUseCase createTaskUseCase,
                           GetTaskUseCase getTaskUseCase,
                           ListTasksUseCase listTasksUseCase,
                           UpdateTaskUseCase updateTaskUseCase,
                           DeleteTaskUseCase deleteTaskUseCase,
                           ChangeTaskStatusUseCase changeTaskStatusUseCase) {
        this.createTaskUseCase = createTaskUseCase;
        this.getTaskUseCase = getTaskUseCase;
        this.listTasksUseCase = listTasksUseCase;
        this.updateTaskUseCase = updateTaskUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
        this.changeTaskStatusUseCase = changeTaskStatusUseCase;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        Task task = createTaskUseCase.execute(
                new CreateTaskCommand(request.title(), request.description(), request.dueDate()));

        return ResponseEntity.created(URI.create("/api/tasks/" + task.id()))
                .body(TaskWebMapper.toResponse(task));
    }

    @GetMapping("/{id}")
    public TaskResponse getById(@PathVariable UUID id) {
        return TaskWebMapper.toResponse(getTaskUseCase.execute(id));
    }

    @GetMapping
    public List<TaskResponse> list() {
        return listTasksUseCase.execute().stream()
                .map(TaskWebMapper::toResponse)
                .toList();
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
        Task task = updateTaskUseCase.execute(
                new UpdateTaskCommand(id, request.title(), request.description(), request.dueDate()));
        return TaskWebMapper.toResponse(task);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeTaskStatusRequest request) {
        Task task = changeTaskStatusUseCase.execute(new ChangeTaskStatusCommand(id, request.action()));
        return TaskWebMapper.toResponse(task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteTaskUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}

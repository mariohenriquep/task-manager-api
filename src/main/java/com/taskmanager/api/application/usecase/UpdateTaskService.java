package com.taskmanager.api.application.usecase;

import com.taskmanager.api.application.command.UpdateTaskCommand;
import com.taskmanager.api.domain.exception.TaskNotFoundException;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class UpdateTaskService implements UpdateTaskUseCase {

    private final TaskRepository taskRepository;
    private final Clock clock;

    public UpdateTaskService(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Override
    public Task execute(UpdateTaskCommand command) {
        Task existing = taskRepository.findById(command.taskId())
                .orElseThrow(() -> new TaskNotFoundException(command.taskId()));

        Task updated = existing.updateDetails(command.title(), command.description(), command.dueDate(), clock);
        return taskRepository.save(updated);
    }
}

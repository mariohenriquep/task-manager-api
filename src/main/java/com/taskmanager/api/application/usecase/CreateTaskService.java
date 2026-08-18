package com.taskmanager.api.application.usecase;

import com.taskmanager.api.application.command.CreateTaskCommand;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class CreateTaskService implements CreateTaskUseCase {

    private final TaskRepository taskRepository;
    private final Clock clock;

    public CreateTaskService(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Override
    public Task execute(CreateTaskCommand command) {
        Task task = Task.create(command.title(), command.description(), command.dueDate(), clock);
        return taskRepository.save(task);
    }
}

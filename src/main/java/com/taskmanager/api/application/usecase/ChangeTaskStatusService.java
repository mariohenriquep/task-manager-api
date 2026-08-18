package com.taskmanager.api.application.usecase;

import com.taskmanager.api.application.command.ChangeTaskStatusCommand;
import com.taskmanager.api.domain.exception.TaskNotFoundException;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class ChangeTaskStatusService implements ChangeTaskStatusUseCase {

    private final TaskRepository taskRepository;
    private final Clock clock;

    public ChangeTaskStatusService(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Override
    public Task execute(ChangeTaskStatusCommand command) {
        Task task = taskRepository.findById(command.taskId())
                .orElseThrow(() -> new TaskNotFoundException(command.taskId()));

        Task transitioned = switch (command.action()) {
            case START -> task.start(clock);
            case COMPLETE -> task.complete(clock);
            case REOPEN -> task.reopen(clock);
        };

        return taskRepository.save(transitioned);
    }
}

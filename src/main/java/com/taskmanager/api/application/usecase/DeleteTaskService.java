package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.exception.TaskNotFoundException;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeleteTaskService implements DeleteTaskUseCase {

    private final TaskRepository taskRepository;

    public DeleteTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void execute(UUID taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }
        taskRepository.deleteById(taskId);
    }
}

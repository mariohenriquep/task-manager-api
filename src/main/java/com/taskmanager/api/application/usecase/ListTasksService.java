package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTasksService implements ListTasksUseCase {

    private final TaskRepository taskRepository;

    public ListTasksService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public List<Task> execute() {
        return taskRepository.findAll();
    }
}

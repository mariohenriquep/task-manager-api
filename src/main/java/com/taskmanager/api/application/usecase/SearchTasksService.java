package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.query.Page;
import com.taskmanager.api.domain.query.TaskQuery;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.springframework.stereotype.Service;

@Service
public class SearchTasksService implements SearchTasksUseCase {

    private final TaskRepository taskRepository;

    public SearchTasksService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public Page<Task> execute(TaskQuery query) {
        return taskRepository.search(query);
    }
}

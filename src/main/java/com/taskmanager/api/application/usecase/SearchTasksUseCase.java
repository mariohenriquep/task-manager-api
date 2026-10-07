package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.query.Page;
import com.taskmanager.api.domain.query.TaskQuery;

public interface SearchTasksUseCase extends UseCase<TaskQuery, Page<Task>> {
}

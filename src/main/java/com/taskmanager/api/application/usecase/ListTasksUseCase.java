package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.model.Task;

import java.util.List;

public interface ListTasksUseCase {

    List<Task> execute();
}

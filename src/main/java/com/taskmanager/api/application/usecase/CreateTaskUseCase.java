package com.taskmanager.api.application.usecase;

import com.taskmanager.api.application.command.CreateTaskCommand;
import com.taskmanager.api.domain.model.Task;

public interface CreateTaskUseCase extends UseCase<CreateTaskCommand, Task> {
}

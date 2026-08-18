package com.taskmanager.api.application.usecase;

import com.taskmanager.api.application.command.UpdateTaskCommand;
import com.taskmanager.api.domain.model.Task;

public interface UpdateTaskUseCase extends UseCase<UpdateTaskCommand, Task> {
}

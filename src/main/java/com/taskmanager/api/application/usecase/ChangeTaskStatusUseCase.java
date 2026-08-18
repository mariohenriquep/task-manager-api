package com.taskmanager.api.application.usecase;

import com.taskmanager.api.application.command.ChangeTaskStatusCommand;
import com.taskmanager.api.domain.model.Task;

public interface ChangeTaskStatusUseCase extends UseCase<ChangeTaskStatusCommand, Task> {
}

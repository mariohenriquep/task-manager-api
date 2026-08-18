package com.taskmanager.api.application.usecase;

import com.taskmanager.api.domain.model.Task;

import java.util.UUID;

public interface GetTaskUseCase extends UseCase<UUID, Task> {
}

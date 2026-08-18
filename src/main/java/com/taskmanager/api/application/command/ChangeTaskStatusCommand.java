package com.taskmanager.api.application.command;

import java.util.UUID;

public record ChangeTaskStatusCommand(UUID taskId, TaskStatusAction action) {
}

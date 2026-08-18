package com.taskmanager.api.infrastructure.web.dto;

import com.taskmanager.api.application.command.TaskStatusAction;
import jakarta.validation.constraints.NotNull;

public record ChangeTaskStatusRequest(

        @NotNull(message = "action must not be null")
        TaskStatusAction action
) {
}

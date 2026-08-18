package com.taskmanager.api.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateTaskCommand(UUID taskId, String title, String description, LocalDate dueDate) {
}

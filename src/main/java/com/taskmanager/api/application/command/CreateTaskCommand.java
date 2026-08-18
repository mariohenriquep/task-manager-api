package com.taskmanager.api.application.command;

import java.time.LocalDate;

public record CreateTaskCommand(String title, String description, LocalDate dueDate) {
}

package com.taskmanager.api.application.command;

/**
 * Application-level vocabulary for the status transitions a client may request.
 * Kept separate from {@code TaskStatus} (the domain's state) because "what the caller
 * asks for" and "what state the aggregate ends up in" are different concerns.
 */
public enum TaskStatusAction {
    START,
    COMPLETE,
    REOPEN
}

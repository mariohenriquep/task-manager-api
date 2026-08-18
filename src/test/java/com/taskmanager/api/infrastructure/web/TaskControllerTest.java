package com.taskmanager.api.infrastructure.web;

import tools.jackson.databind.ObjectMapper;
import com.taskmanager.api.application.command.ChangeTaskStatusCommand;
import com.taskmanager.api.application.command.CreateTaskCommand;
import com.taskmanager.api.application.command.TaskStatusAction;
import com.taskmanager.api.application.command.UpdateTaskCommand;
import com.taskmanager.api.application.usecase.ChangeTaskStatusUseCase;
import com.taskmanager.api.application.usecase.CreateTaskUseCase;
import com.taskmanager.api.application.usecase.DeleteTaskUseCase;
import com.taskmanager.api.application.usecase.GetTaskUseCase;
import com.taskmanager.api.application.usecase.ListTasksUseCase;
import com.taskmanager.api.application.usecase.UpdateTaskUseCase;
import com.taskmanager.api.domain.exception.InvalidTaskException;
import com.taskmanager.api.domain.exception.InvalidTaskStatusTransitionException;
import com.taskmanager.api.domain.exception.TaskNotFoundException;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.model.TaskStatus;
import com.taskmanager.api.infrastructure.web.controller.TaskController;
import com.taskmanager.api.infrastructure.web.dto.ChangeTaskStatusRequest;
import com.taskmanager.api.infrastructure.web.dto.CreateTaskRequest;
import com.taskmanager.api.infrastructure.web.dto.UpdateTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final Clock FIXED_CLOCK = Clock.systemUTC();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateTaskUseCase createTaskUseCase;

    @MockitoBean
    private GetTaskUseCase getTaskUseCase;

    @MockitoBean
    private ListTasksUseCase listTasksUseCase;

    @MockitoBean
    private UpdateTaskUseCase updateTaskUseCase;

    @MockitoBean
    private DeleteTaskUseCase deleteTaskUseCase;

    @MockitoBean
    private ChangeTaskStatusUseCase changeTaskStatusUseCase;

    @Test
    void createsATaskAndReturns201WithLocationHeader() throws Exception {
        Task task = Task.create("Write tests", "Follow TDD", LocalDate.of(2026, 9, 1), FIXED_CLOCK);
        when(createTaskUseCase.execute(any(CreateTaskCommand.class))).thenReturn(task);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTaskRequest("Write tests", "Follow TDD", LocalDate.of(2026, 9, 1)))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tasks/" + task.id()))
                .andExpect(jsonPath("$.id").value(task.id().toString()))
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void rejectsCreateWithBlankTitle() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTaskRequest("  ", null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returns400WhenDomainRejectsAnInvalidTaskOutsideBeanValidation() throws Exception {
        // A domain invariant can differ from the DTO's own @Size/@NotBlank bounds, so
        // InvalidTaskException can reach the controller without MethodArgumentNotValidException
        // ever being raised - this exercises that path specifically, not the @Valid one above.
        when(createTaskUseCase.execute(any(CreateTaskCommand.class)))
                .thenThrow(new InvalidTaskException("Task title must not be blank"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTaskRequest("Valid title", null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Task title must not be blank"));
    }

    @Test
    void returnsATaskById() throws Exception {
        Task task = Task.create("Task", null, null, FIXED_CLOCK);
        when(getTaskUseCase.execute(task.id())).thenReturn(task);

        mockMvc.perform(get("/api/tasks/{id}", task.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.id().toString()));
    }

    @Test
    void returns404WhenTaskNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getTaskUseCase.execute(id)).thenThrow(new TaskNotFoundException(id));

        mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listsAllTasks() throws Exception {
        Task task = Task.create("Task", null, null, FIXED_CLOCK);
        when(listTasksUseCase.execute()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(task.id().toString()));
    }

    @Test
    void updatesATask() throws Exception {
        Task task = Task.create("Task", null, null, FIXED_CLOCK)
                .updateDetails("New title", "New desc", null, FIXED_CLOCK);
        when(updateTaskUseCase.execute(any(UpdateTaskCommand.class))).thenReturn(task);

        mockMvc.perform(put("/api/tasks/{id}", task.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateTaskRequest("New title", "New desc", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"));
    }

    @Test
    void changesTaskStatus() throws Exception {
        Task task = Task.create("Task", null, null, FIXED_CLOCK).start(FIXED_CLOCK);
        when(changeTaskStatusUseCase.execute(any(ChangeTaskStatusCommand.class))).thenReturn(task);

        mockMvc.perform(patch("/api/tasks/{id}/status", task.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeTaskStatusRequest(TaskStatusAction.START))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void returns409OnInvalidStatusTransition() throws Exception {
        UUID id = UUID.randomUUID();
        when(changeTaskStatusUseCase.execute(any(ChangeTaskStatusCommand.class)))
                .thenThrow(new InvalidTaskStatusTransitionException(TaskStatus.DONE, TaskStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/tasks/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeTaskStatusRequest(TaskStatusAction.START))))
                .andExpect(status().isConflict());
    }

    @Test
    void deletesATaskAndReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/tasks/{id}", id))
                .andExpect(status().isNoContent());

        verify(deleteTaskUseCase).execute(eq(id));
    }

    @Test
    void returns404WhenDeletingMissingTask() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new TaskNotFoundException(id)).when(deleteTaskUseCase).execute(id);

        mockMvc.perform(delete("/api/tasks/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void content_type_is_json_for_error_responses() throws Exception {
        UUID id = UUID.randomUUID();
        when(getTaskUseCase.execute(id)).thenThrow(new TaskNotFoundException(id));

        mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}

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
import com.taskmanager.api.application.usecase.SearchTasksUseCase;
import com.taskmanager.api.application.usecase.UpdateTaskUseCase;
import com.taskmanager.api.domain.exception.InvalidTaskException;
import com.taskmanager.api.domain.exception.InvalidTaskQueryException;
import com.taskmanager.api.domain.exception.InvalidTaskStatusTransitionException;
import com.taskmanager.api.domain.exception.TaskNotFoundException;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.model.TaskStatus;
import com.taskmanager.api.domain.query.Page;
import com.taskmanager.api.domain.query.PageRequest;
import com.taskmanager.api.domain.query.SortDirection;
import com.taskmanager.api.domain.query.TaskQuery;
import com.taskmanager.api.domain.query.TaskSort;
import com.taskmanager.api.domain.query.TaskSortField;
import com.taskmanager.api.infrastructure.web.controller.TaskController;
import com.taskmanager.api.infrastructure.web.dto.ChangeTaskStatusRequest;
import com.taskmanager.api.infrastructure.web.dto.CreateTaskRequest;
import com.taskmanager.api.infrastructure.web.dto.UpdateTaskRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    private SearchTasksUseCase searchTasksUseCase;

    @MockitoBean
    private UpdateTaskUseCase updateTaskUseCase;

    @MockitoBean
    private DeleteTaskUseCase deleteTaskUseCase;

    @MockitoBean
    private ChangeTaskStatusUseCase changeTaskStatusUseCase;

    @Test
    void createsATaskAndReturns201WithLocationHeader() throws Exception {
        // Arrange
        Task task = Task.create("Write tests", "Follow TDD", LocalDate.of(2026, 9, 1), FIXED_CLOCK);
        when(createTaskUseCase.execute(any(CreateTaskCommand.class))).thenReturn(task);
        String requestBody = objectMapper.writeValueAsString(
                new CreateTaskRequest("Write tests", "Follow TDD", LocalDate.of(2026, 9, 1)));

        // Act
        ResultActions result = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tasks/" + task.id()))
                .andExpect(jsonPath("$.id").value(task.id().toString()))
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void rejectsCreateWithBlankTitle() throws Exception {
        // Arrange
        String requestBody = objectMapper.writeValueAsString(new CreateTaskRequest("  ", null, null));

        // Act
        ResultActions result = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returns400WhenDomainRejectsAnInvalidTaskOutsideBeanValidation() throws Exception {
        // Arrange
        // A domain invariant can differ from the DTO's own @Size/@NotBlank bounds, so
        // InvalidTaskException can reach the controller without MethodArgumentNotValidException
        // ever being raised - this exercises that path specifically, not the @Valid one above.
        when(createTaskUseCase.execute(any(CreateTaskCommand.class)))
                .thenThrow(new InvalidTaskException("Task title must not be blank"));
        String requestBody = objectMapper.writeValueAsString(new CreateTaskRequest("Valid title", null, null));

        // Act
        ResultActions result = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Task title must not be blank"));
    }

    @Test
    void returnsATaskById() throws Exception {
        // Arrange
        Task task = Task.create("Task", null, null, FIXED_CLOCK);
        when(getTaskUseCase.execute(task.id())).thenReturn(task);

        // Act
        ResultActions result = mockMvc.perform(get("/api/tasks/{id}", task.id()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.id().toString()));
    }

    @Test
    void returns404WhenTaskNotFound() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        when(getTaskUseCase.execute(id)).thenThrow(new TaskNotFoundException(id));

        // Act
        ResultActions result = mockMvc.perform(get("/api/tasks/{id}", id));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Nested
    class ListingTasks {

        private static final String TASKS_PATH = "/api/tasks";

        @Test
        void withNoParametersReturnsTheEnvelopeAndSearchesWithTheDefaultQuery() throws Exception {
            // Arrange
            Task task = Task.create("Task", null, null, FIXED_CLOCK);
            when(searchTasksUseCase.execute(any(TaskQuery.class))).thenReturn(new Page<>(List.of(task), 0, 20, 1));

            // Act
            ResultActions result = mockMvc.perform(get(TASKS_PATH));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(task.id().toString()))
                    .andExpect(jsonPath("$.content[0].title").value("Task"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
            assertThat(capturedQuery()).isEqualTo(TaskQuery.all());
        }

        @Test
        void returnsTheEnvelopeMetadataOfTheUseCasePage() throws Exception {
            // Arrange
            Task task = Task.create("Task", null, null, FIXED_CLOCK);
            when(searchTasksUseCase.execute(any(TaskQuery.class))).thenReturn(new Page<>(List.of(task), 2, 5, 11));

            // Act
            ResultActions result = mockMvc.perform(get(TASKS_PATH).param("page", "2").param("size", "5"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        void returnsAnEmptyContentArrayWhenNothingMatches() throws Exception {
            // Arrange
            when(searchTasksUseCase.execute(any(TaskQuery.class))).thenReturn(new Page<>(List.of(), 0, 20, 0));

            // Act
            ResultActions result = mockMvc.perform(get(TASKS_PATH));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0));
        }

        @Test
        void bindsTheStatusParameter() throws Exception {
            // Arrange
            givenAnEmptyPage();

            // Act
            ResultActions result = mockMvc.perform(get(TASKS_PATH).param("status", "IN_PROGRESS"));

            // Assert
            result.andExpect(status().isOk());
            assertThat(capturedQuery().status()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        void bindsTheDueAfterParameter() throws Exception {
            // Arrange
            givenAnEmptyPage();

            // Act
            ResultActions result = mockMvc.perform(get(TASKS_PATH).param("dueAfter", "2026-10-01"));

            // Assert
            result.andExpect(status().isOk());
            assertThat(capturedQuery().dueAfter()).isEqualTo(LocalDate.of(2026, 10, 1));
            assertThat(capturedQuery().dueBefore()).isNull();
        }

        @Test
        void bindsTheDueBeforeParameter() throws Exception {
            // Arrange
            givenAnEmptyPage();

            // Act
            ResultActions result = mockMvc.perform(get(TASKS_PATH).param("dueBefore", "2026-11-01"));

            // Assert
            result.andExpect(status().isOk());
            assertThat(capturedQuery().dueBefore()).isEqualTo(LocalDate.of(2026, 11, 1));
            assertThat(capturedQuery().dueAfter()).isNull();
        }

        @Test
        void bindsTheSortParameter() throws Exception {
            // Arrange
            givenAnEmptyPage();

            // Act
            ResultActions result = mockMvc.perform(get(TASKS_PATH).param("sort", "dueDate,asc"));

            // Assert
            result.andExpect(status().isOk());
            assertThat(capturedQuery().sort()).isEqualTo(new TaskSort(TaskSortField.DUE_DATE, SortDirection.ASC));
        }

        @Test
        void bindsThePageAndSizeParameters() throws Exception {
            // Arrange
            givenAnEmptyPage();

            // Act
            ResultActions result = mockMvc.perform(get(TASKS_PATH).param("page", "3").param("size", "50"));

            // Assert
            result.andExpect(status().isOk());
            assertThat(capturedQuery().pageRequest()).isEqualTo(new PageRequest(3, 50));
        }

        @Test
        void bindsAllParametersTogether() throws Exception {
            // Arrange
            givenAnEmptyPage();

            // Act
            ResultActions result = mockMvc.perform(get(TASKS_PATH)
                    .param("status", "TODO")
                    .param("dueAfter", "2026-10-01")
                    .param("dueBefore", "2026-11-01")
                    .param("sort", "title,desc")
                    .param("page", "1")
                    .param("size", "10"));

            // Assert
            result.andExpect(status().isOk());
            assertThat(capturedQuery()).isEqualTo(new TaskQuery(
                    TaskStatus.TODO,
                    LocalDate.of(2026, 10, 1),
                    LocalDate.of(2026, 11, 1),
                    new PageRequest(1, 10),
                    new TaskSort(TaskSortField.TITLE, SortDirection.DESC)));
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "sort=priority,asc                     | Unknown sort field 'priority'",
                "sort=title,sideways                   | Unknown sort direction 'sideways'",
                "sort=title,asc,extra                  | Invalid sort 'title,asc,extra'",
                "dueAfter=2026-10-02&dueBefore=2026-10-01 | dueAfter must be before dueBefore",
                "dueAfter=2026-10-01&dueBefore=2026-10-01 | dueAfter must be before dueBefore",
                "page=-1                               | Page index must not be negative",
                "size=0                                | Page size must be between 1 and 100",
                "size=101                              | Page size must be between 1 and 100"
        })
        void rejectsAnInvalidQueryWith400AndTheDomainMessage(String queryString, String expectedMessage)
                throws Exception {
            // Arrange
            String url = TASKS_PATH + "?" + queryString;

            // Act
            ResultActions result = mockMvc.perform(get(url));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(expectedMessage)))
                    .andExpect(jsonPath("$.path").value(TASKS_PATH));
            verify(searchTasksUseCase, never()).execute(any(TaskQuery.class));
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "status=BLOCKED     | status",
                "status=todo        | status",
                "dueAfter=yesterday | dueAfter",
                "dueAfter=2026-13-45 | dueAfter",
                "dueBefore=01/11/2026 | dueBefore",
                "page=abc           | page",
                "size=abc           | size",
                "page=1.5           | page"
        })
        void rejectsAnUnparsableValueWith400NamingTheParameter(String queryString, String parameter)
                throws Exception {
            // Arrange
            String url = TASKS_PATH + "?" + queryString;

            // Act
            ResultActions result = mockMvc.perform(get(url));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter '" + parameter + "'"))
                    .andExpect(jsonPath("$.path").value(TASKS_PATH));
            verify(searchTasksUseCase, never()).execute(any(TaskQuery.class));
        }

        @Test
        void mapsAnInvalidTaskQueryRaisedByTheUseCaseTo400WithItsMessage() throws Exception {
            // Arrange
            when(searchTasksUseCase.execute(any(TaskQuery.class)))
                    .thenThrow(new InvalidTaskQueryException("query rejected downstream"));

            // Act
            ResultActions result = mockMvc.perform(get(TASKS_PATH));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("query rejected downstream"));
        }

        private void givenAnEmptyPage() {
            when(searchTasksUseCase.execute(any(TaskQuery.class))).thenReturn(new Page<>(List.of(), 0, 20, 0));
        }

        private TaskQuery capturedQuery() {
            ArgumentCaptor<TaskQuery> captor = ArgumentCaptor.forClass(TaskQuery.class);
            verify(searchTasksUseCase).execute(captor.capture());
            return captor.getValue();
        }
    }

    @Test
    void updatesATask() throws Exception {
        // Arrange
        Task task = Task.create("Task", null, null, FIXED_CLOCK)
                .updateDetails("New title", "New desc", null, FIXED_CLOCK);
        when(updateTaskUseCase.execute(any(UpdateTaskCommand.class))).thenReturn(task);
        String requestBody = objectMapper.writeValueAsString(new UpdateTaskRequest("New title", "New desc", null));

        // Act
        ResultActions result = mockMvc.perform(put("/api/tasks/{id}", task.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"));
    }

    @Test
    void changesTaskStatus() throws Exception {
        // Arrange
        Task task = Task.create("Task", null, null, FIXED_CLOCK).start(FIXED_CLOCK);
        when(changeTaskStatusUseCase.execute(any(ChangeTaskStatusCommand.class))).thenReturn(task);
        String requestBody = objectMapper.writeValueAsString(new ChangeTaskStatusRequest(TaskStatusAction.START));

        // Act
        ResultActions result = mockMvc.perform(patch("/api/tasks/{id}/status", task.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void returns409OnInvalidStatusTransition() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        when(changeTaskStatusUseCase.execute(any(ChangeTaskStatusCommand.class)))
                .thenThrow(new InvalidTaskStatusTransitionException(TaskStatus.DONE, TaskStatus.IN_PROGRESS));
        String requestBody = objectMapper.writeValueAsString(new ChangeTaskStatusRequest(TaskStatusAction.START));

        // Act
        ResultActions result = mockMvc.perform(patch("/api/tasks/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Assert
        result.andExpect(status().isConflict());
    }

    @Test
    void deletesATaskAndReturns204() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        ResultActions result = mockMvc.perform(delete("/api/tasks/{id}", id));

        // Assert
        result.andExpect(status().isNoContent());
        verify(deleteTaskUseCase).execute(eq(id));
    }

    @Test
    void returns404WhenDeletingMissingTask() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new TaskNotFoundException(id)).when(deleteTaskUseCase).execute(id);

        // Act
        ResultActions result = mockMvc.perform(delete("/api/tasks/{id}", id));

        // Assert
        result.andExpect(status().isNotFound());
    }

    @Test
    void content_type_is_json_for_error_responses() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        when(getTaskUseCase.execute(id)).thenThrow(new TaskNotFoundException(id));

        // Act
        ResultActions result = mockMvc.perform(get("/api/tasks/{id}", id));

        // Assert
        result.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}

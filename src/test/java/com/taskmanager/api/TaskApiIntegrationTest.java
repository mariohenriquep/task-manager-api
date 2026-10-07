package com.taskmanager.api;

import com.jayway.jsonpath.JsonPath;
import com.taskmanager.api.application.command.TaskStatusAction;
import com.taskmanager.api.infrastructure.AbstractPostgresIntegrationTest;
import com.taskmanager.api.infrastructure.web.dto.ChangeTaskStatusRequest;
import com.taskmanager.api.infrastructure.web.dto.CreateTaskRequest;
import com.taskmanager.api.infrastructure.web.dto.UpdateTaskRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test: boots the whole application (real Flyway schema, real JPA adapter, real
 * PostgreSQL from Testcontainers) and drives it through the HTTP API with MockMvc. No mocks.
 *
 * <p>The Postgres container is shared by every test class in the JVM and its rows persist, so
 * each test creates its own task through the API and only ever asserts on that task's id -
 * never on the table being empty or on exact list sizes.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String BASE_URL = "/api/tasks";
    private static final String TITLE = "Write the end-to-end test";
    private static final String DESCRIPTION = "Drive the real stack over HTTP";
    private static final LocalDate DUE_DATE = LocalDate.of(2030, 1, 15);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class Lifecycle {

        @Test
        void createReturns201WithLocationAndPersistedFields() throws Exception {
            // Arrange
            String body = objectMapper.writeValueAsString(new CreateTaskRequest(TITLE, DESCRIPTION, DUE_DATE));

            // Act
            ResultActions result = mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));

            // Assert
            String id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
            result.andExpect(status().isCreated())
                    .andExpect(header().string("Location", BASE_URL + "/" + id))
                    .andExpect(jsonPath("$.title").value(TITLE))
                    .andExpect(jsonPath("$.description").value(DESCRIPTION))
                    .andExpect(jsonPath("$.dueDate").value("2030-01-15"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        void getByIdReturnsTheCreatedTask() throws Exception {
            // Arrange
            String id = createTask();

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL + "/" + id));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.title").value(TITLE))
                    .andExpect(jsonPath("$.description").value(DESCRIPTION))
                    .andExpect(jsonPath("$.dueDate").value("2030-01-15"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        void listContainsTheCreatedTask() throws Exception {
            // Arrange
            String id = createTask();

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == '" + id + "')]").isNotEmpty())
                    .andExpect(jsonPath("$[?(@.id == '" + id + "')].title").value(TITLE));
        }

        @Test
        void updateChangesDetailsAndKeepsStatusAndCreatedAt() throws Exception {
            // Arrange
            String id = createTask();
            String before = getTaskBody(id);
            String updateBody = objectMapper.writeValueAsString(
                    new UpdateTaskRequest("Updated title", "Updated description", LocalDate.of(2031, 6, 30)));

            // Act
            ResultActions result = mockMvc.perform(put(BASE_URL + "/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateBody));

            // Assert
            String after = result.andReturn().getResponse().getContentAsString();
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.title").value("Updated title"))
                    .andExpect(jsonPath("$.description").value("Updated description"))
                    .andExpect(jsonPath("$.dueDate").value("2031-06-30"))
                    .andExpect(jsonPath("$.status").value("TODO"));
            assertThat(instantAt(after, "$.createdAt")).isEqualTo(instantAt(before, "$.createdAt"));
            assertThat(instantAt(after, "$.updatedAt")).isAfterOrEqualTo(instantAt(before, "$.updatedAt"));
        }

        @Test
        void updatedValuesArePersistedAndVisibleOnNextGet() throws Exception {
            // Arrange
            String id = createTask();
            mockMvc.perform(put(BASE_URL + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateTaskRequest("Persisted title", "Persisted description",
                                            LocalDate.of(2031, 6, 30)))))
                    .andExpect(status().isOk());

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL + "/" + id));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Persisted title"))
                    .andExpect(jsonPath("$.description").value("Persisted description"))
                    .andExpect(jsonPath("$.dueDate").value("2031-06-30"))
                    .andExpect(jsonPath("$.status").value("TODO"));
        }

        @Test
        void deleteReturns204() throws Exception {
            // Arrange
            String id = createTask();

            // Act
            ResultActions result = mockMvc.perform(delete(BASE_URL + "/" + id));

            // Assert
            result.andExpect(status().isNoContent());
        }

        @Test
        void deletedTaskIsNoLongerFound() throws Exception {
            // Arrange
            String id = createTask();
            mockMvc.perform(delete(BASE_URL + "/" + id)).andExpect(status().isNoContent());

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL + "/" + id));

            // Assert
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    class StatusTransitions {

        @Test
        void startMovesTodoTaskToInProgress() throws Exception {
            // Arrange
            String id = createTask();

            // Act
            ResultActions result = changeStatus(id, TaskStatusAction.START);

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        void completeMovesInProgressTaskToDone() throws Exception {
            // Arrange
            String id = createTask();
            changeStatus(id, TaskStatusAction.START).andExpect(status().isOk());

            // Act
            ResultActions result = changeStatus(id, TaskStatusAction.COMPLETE);

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.status").value("DONE"));
        }

        @Test
        void reopenMovesDoneTaskBackToTodo() throws Exception {
            // Arrange
            String id = createTask();
            changeStatus(id, TaskStatusAction.COMPLETE).andExpect(status().isOk());

            // Act
            ResultActions result = changeStatus(id, TaskStatusAction.REOPEN);

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.status").value("TODO"));
        }

        @Test
        void statusChangeIsPersistedAndVisibleOnNextGet() throws Exception {
            // Arrange
            String id = createTask();
            changeStatus(id, TaskStatusAction.START).andExpect(status().isOk());

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL + "/" + id));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }
    }

    @Nested
    class Errors {

        @Test
        void getUnknownIdReturns404ErrorResponse() throws Exception {
            // Arrange
            UUID unknownId = UUID.randomUUID();

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL + "/" + unknownId));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Task not found: " + unknownId))
                    .andExpect(jsonPath("$.path").value(BASE_URL + "/" + unknownId))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        void completingAnAlreadyDoneTaskReturns409ErrorResponse() throws Exception {
            // Arrange
            String id = createTask();
            changeStatus(id, TaskStatusAction.COMPLETE).andExpect(status().isOk());

            // Act
            ResultActions result = changeStatus(id, TaskStatusAction.COMPLETE);

            // Assert
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.message").value("Cannot move task from status DONE to DONE"))
                    .andExpect(jsonPath("$.path").value(BASE_URL + "/" + id + "/status"));
        }

        @Test
        void invalidTransitionDoesNotChangeThePersistedStatus() throws Exception {
            // Arrange
            String id = createTask();
            changeStatus(id, TaskStatusAction.COMPLETE).andExpect(status().isOk());
            changeStatus(id, TaskStatusAction.COMPLETE).andExpect(status().isConflict());

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL + "/" + id));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DONE"));
        }

        @Test
        void createWithBlankTitleReturns400ErrorResponseWithDetails() throws Exception {
            // Arrange
            String body = objectMapper.writeValueAsString(new CreateTaskRequest("   ", DESCRIPTION, DUE_DATE));

            // Act
            ResultActions result = mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.path").value(BASE_URL))
                    .andExpect(jsonPath("$.details[0]").value("title: title must not be blank"));
        }
    }

    /** Creates a task through the API and returns its id. */
    private String createTask() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateTaskRequest(TITLE, DESCRIPTION, DUE_DATE));
        String response = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String getTaskBody(String id) throws Exception {
        return mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions changeStatus(String id, TaskStatusAction action) throws Exception {
        return mockMvc.perform(patch(BASE_URL + "/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangeTaskStatusRequest(action))));
    }

    private static Instant instantAt(String json, String path) {
        String value = JsonPath.read(json, path);
        return Instant.parse(value);
    }
}

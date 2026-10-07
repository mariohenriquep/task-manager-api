package com.taskmanager.api.infrastructure.web;

import com.taskmanager.api.application.command.CreateTaskCommand;
import com.taskmanager.api.application.usecase.ChangeTaskStatusUseCase;
import com.taskmanager.api.application.usecase.CreateTaskUseCase;
import com.taskmanager.api.application.usecase.DeleteTaskUseCase;
import com.taskmanager.api.application.usecase.GetTaskUseCase;
import com.taskmanager.api.application.usecase.SearchTasksUseCase;
import com.taskmanager.api.application.usecase.UpdateTaskUseCase;
import com.taskmanager.api.infrastructure.web.controller.TaskController;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that errors raised by the framework itself (before or around the controller) and
 * unexpected failures are translated into the same {@code ErrorResponse} JSON shape as the
 * domain errors, with the right status code and without leaking internals.
 */
@WebMvcTest(TaskController.class)
class TaskControllerErrorHandlingTest {

    private static final String TASKS_PATH = "/api/tasks";

    @Autowired
    private MockMvc mockMvc;

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

    @Nested
    class UnreadableRequests {

        @Test
        void malformedJsonBodyReturns400() throws Exception {
            // Arrange
            String malformedJson = "{\"title\": ";

            // Act
            ResultActions result = mockMvc.perform(post(TASKS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.message").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.path").value(TASKS_PATH))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void missingRequestBodyReturns400() throws Exception {
            // Arrange
            // (no body and no content type on purpose)

            // Act
            ResultActions result = mockMvc.perform(post(TASKS_PATH)
                    .contentType(MediaType.APPLICATION_JSON));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.message").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.path").value(TASKS_PATH))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void unknownStatusActionReturns400() throws Exception {
            // Arrange
            String path = TASKS_PATH + "/" + UUID.randomUUID() + "/status";
            String unknownAction = "{\"action\":\"EXPLODE\"}";

            // Act
            ResultActions result = mockMvc.perform(patch(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(unknownAction));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.message").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.path").value(path))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void nonUuidIdReturns400() throws Exception {
            // Arrange
            String path = TASKS_PATH + "/not-a-uuid";

            // Act
            ResultActions result = mockMvc.perform(get(path));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.message").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.path").value(path))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    class UnsupportedRequests {

        @Test
        void unsupportedHttpMethodReturns405() throws Exception {
            // Arrange
            // DELETE is only mapped on /api/tasks/{id}, not on the collection

            // Act
            ResultActions result = mockMvc.perform(delete(TASKS_PATH));

            // Assert
            result.andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405))
                    .andExpect(jsonPath("$.error").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.message").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.path").value(TASKS_PATH))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void unknownRouteKeepsItsStatusInsteadOfBecoming500() throws Exception {
            // Arrange
            String unknownPath = "/api/does-not-exist";

            // Act
            ResultActions result = mockMvc.perform(get(unknownPath));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.path").value(unknownPath))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void unsupportedMediaTypeReturns415() throws Exception {
            // Arrange
            String plainTextBody = "just some text";

            // Act
            ResultActions result = mockMvc.perform(post(TASKS_PATH)
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(plainTextBody));

            // Assert
            result.andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.status").value(415))
                    .andExpect(jsonPath("$.error").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.message").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.path").value(TASKS_PATH))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    class UnexpectedFailures {

        @Test
        void unexpectedExceptionReturns500WithoutLeakingInternals() throws Exception {
            // Arrange
            when(createTaskUseCase.execute(any(CreateTaskCommand.class)))
                    .thenThrow(new IllegalStateException("db password is hunter2"));
            String validBody = "{\"title\":\"Write tests\"}";

            // Act
            ResultActions result = mockMvc.perform(post(TASKS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody));

            // Assert
            result.andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.message").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.path").value(TASKS_PATH))
                    .andExpect(jsonPath("$.timestamp").exists());
            String responseBody = result.andReturn().getResponse().getContentAsString();
            assertThat(responseBody)
                    .doesNotContain("hunter2")
                    .doesNotContain("IllegalStateException")
                    .doesNotContain("at com.taskmanager");
        }
    }

    @Nested
    class ValidationFailures {

        @Test
        void overlongTitleReturns400WithFieldDetail() throws Exception {
            // Arrange
            String overlongTitle = "x".repeat(201);
            String body = "{\"title\":\"" + overlongTitle + "\"}";

            // Act
            ResultActions result = mockMvc.perform(post(TASKS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.message").value(not(emptyOrNullString())))
                    .andExpect(jsonPath("$.path").value(TASKS_PATH))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.details").value(hasItem(containsString("title"))));
        }
    }
}

package com.taskmanager.api;

import com.jayway.jsonpath.JsonPath;
import com.taskmanager.api.application.command.TaskStatusAction;
import com.taskmanager.api.infrastructure.AbstractPostgresIntegrationTest;
import com.taskmanager.api.infrastructure.web.dto.ChangeTaskStatusRequest;
import com.taskmanager.api.infrastructure.web.dto.CreateTaskRequest;
import com.taskmanager.api.infrastructure.web.dto.UpdateTaskRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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
 * <p>This class gets its own fresh PostgreSQL container (one per test class), but within it
 * every request commits for real and the rows persist from one test to the next. So each test
 * creates its own task through the API and only ever asserts on that task's id - never on the
 * table being empty or on exact list sizes - which keeps the tests independent of their order.
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
                    .andExpect(jsonPath("$.content[?(@.id == '" + id + "')]").isNotEmpty())
                    .andExpect(jsonPath("$.content[?(@.id == '" + id + "')].title").value(TITLE));
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

    /**
     * Search through the real stack. Rows persist across tests in this class, so every test builds
     * its own tasks inside a due-date window nobody else uses (a distinct far-future year and
     * month per test) and scopes its query to that window, so it only ever sees its own tasks.
     * Only {@link #undatedTasksSortLastInBothDirections()} creates a task without a due date, and
     * it is the only test that may: undated tasks cannot be scoped by a date window.
     */
    @Nested
    class Search {

        @Test
        void withNoParametersReturnsTheEnvelopeWithDefaults() throws Exception {
            // Arrange
            String older = createTask("Older", LocalDate.of(2040, 1, 1));
            String newer = createTask("Newer", LocalDate.of(2040, 1, 1));

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.content.length()").value(lessThanOrEqualTo(20)))
                    .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)))
                    .andExpect(jsonPath("$.totalPages").value(greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.content[0].id").value(newer))
                    .andExpect(jsonPath("$.content[1].id").value(older));
        }

        @Test
        void filtersByStatus() throws Exception {
            // Arrange
            LocalDate due = LocalDate.of(2041, 1, 10);
            createTask("Todo", due);
            String inProgress = createTask("In progress", due);
            changeStatus(inProgress, TaskStatusAction.START).andExpect(status().isOk());
            String done = createTask("Done", due);
            changeStatus(done, TaskStatusAction.COMPLETE).andExpect(status().isOk());

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL)
                    .param("status", "IN_PROGRESS")
                    .param("dueAfter", "2041-01-01")
                    .param("dueBefore", "2041-02-01"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").value(contains(inProgress)))
                    .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void filtersByDueDateWindowWithExclusiveBounds() throws Exception {
            // Arrange
            createTask("On the lower bound", LocalDate.of(2042, 2, 1));
            String inside1 = createTask("Inside 1", LocalDate.of(2042, 2, 10));
            String inside2 = createTask("Inside 2", LocalDate.of(2042, 2, 28));
            createTask("On the upper bound", LocalDate.of(2042, 3, 1));

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL)
                    .param("dueAfter", "2042-02-01")
                    .param("dueBefore", "2042-03-01")
                    .param("sort", "dueDate,asc"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").value(contains(inside1, inside2)))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        void aSingleBoundFiltersOnThatSideOnly() throws Exception {
            // Arrange
            String early = createTask("Early", LocalDate.of(2046, 6, 5));
            String late = createTask("Late", LocalDate.of(2046, 6, 25));

            // Act
            ResultActions after = mockMvc.perform(get(BASE_URL)
                    .param("dueAfter", "2046-06-10").param("dueBefore", "2046-07-01"));
            ResultActions before = mockMvc.perform(get(BASE_URL)
                    .param("dueAfter", "2046-05-31").param("dueBefore", "2046-06-10"));

            // Assert
            after.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").value(contains(late)));
            before.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").value(contains(early)));
        }

        @Test
        void sortsByDueDateAscending() throws Exception {
            // Arrange
            String third = createTask("Third", LocalDate.of(2043, 3, 20));
            String first = createTask("First", LocalDate.of(2043, 3, 5));
            String second = createTask("Second", LocalDate.of(2043, 3, 12));

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL)
                    .param("dueAfter", "2043-02-28").param("dueBefore", "2043-04-01")
                    .param("sort", "dueDate,asc"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").value(contains(first, second, third)));
        }

        @Test
        void sortsByDueDateDescending() throws Exception {
            // Arrange
            String third = createTask("Third", LocalDate.of(2044, 4, 20));
            String first = createTask("First", LocalDate.of(2044, 4, 5));
            String second = createTask("Second", LocalDate.of(2044, 4, 12));

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL)
                    .param("dueAfter", "2044-03-31").param("dueBefore", "2044-05-01")
                    .param("sort", "dueDate,desc"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").value(contains(third, second, first)));
        }

        @Test
        void sortsByTitleWhenNoDirectionIsGivenAscending() throws Exception {
            // Arrange
            LocalDate due = LocalDate.of(2047, 7, 7);
            String banana = createTask("Banana", due);
            String cherry = createTask("Cherry", due);
            String apple = createTask("Apple", due);

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL)
                    .param("dueAfter", "2047-07-06").param("dueBefore", "2047-07-08")
                    .param("sort", "title"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").value(contains(apple, banana, cherry)));
        }

        @Test
        void undatedTasksSortLastInBothDirections() throws Exception {
            // Arrange
            // The only undated task in the whole table, so it is the very last row of either order.
            String undated = createTask("No due date", null);
            createTask("Dated", LocalDate.of(2048, 8, 8));
            long total = totalElements(mockMvc.perform(get(BASE_URL).param("size", "1")));

            // Act
            ResultActions ascending = mockMvc.perform(get(BASE_URL)
                    .param("sort", "dueDate,asc").param("size", "1").param("page", String.valueOf(total - 1)));
            ResultActions descending = mockMvc.perform(get(BASE_URL)
                    .param("sort", "dueDate,desc").param("size", "1").param("page", String.valueOf(total - 1)));

            // Assert
            ascending.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(undated));
            descending.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(undated));
        }

        @Test
        void pagesThroughTheResultWithTotalsAndAnEmptyPageBeyondTheEnd() throws Exception {
            // Arrange
            String t1 = createTask("P1", LocalDate.of(2045, 5, 1));
            String t2 = createTask("P2", LocalDate.of(2045, 5, 2));
            String t3 = createTask("P3", LocalDate.of(2045, 5, 3));
            String t4 = createTask("P4", LocalDate.of(2045, 5, 4));
            String t5 = createTask("P5", LocalDate.of(2045, 5, 5));
            MockHttpServletRequestBuilder first = pagedRequest(0);
            MockHttpServletRequestBuilder middle = pagedRequest(1);
            MockHttpServletRequestBuilder last = pagedRequest(2);
            MockHttpServletRequestBuilder beyond = pagedRequest(3);

            // Act
            ResultActions firstPage = mockMvc.perform(first);
            ResultActions middlePage = mockMvc.perform(middle);
            ResultActions lastPage = mockMvc.perform(last);
            ResultActions beyondPage = mockMvc.perform(beyond);

            // Assert
            firstPage.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").value(contains(t1, t2)))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3));
            middlePage.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").value(contains(t3, t4)))
                    .andExpect(jsonPath("$.page").value(1));
            lastPage.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").value(contains(t5)))
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3));
            beyondPage.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page").value(3))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        void combinesStatusDateWindowSortAndPaging() throws Exception {
            // Arrange
            LocalDate due = LocalDate.of(2049, 9, 9);
            String doneB = createTask("B", due);
            String doneA = createTask("A", due);
            String doneC = createTask("C", due);
            createTask("Z", due);
            for (String id : List.of(doneA, doneB, doneC)) {
                changeStatus(id, TaskStatusAction.COMPLETE).andExpect(status().isOk());
            }

            // Act
            ResultActions result = mockMvc.perform(get(BASE_URL)
                    .param("status", "DONE")
                    .param("dueAfter", "2049-09-08")
                    .param("dueBefore", "2049-09-10")
                    .param("sort", "title,desc")
                    .param("page", "1")
                    .param("size", "2"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").value(contains(doneA)))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "sort=priority,asc                         | Unknown sort field 'priority'",
                "sort=title,sideways                       | Unknown sort direction 'sideways'",
                "status=BLOCKED                            | Invalid value for parameter 'status'",
                "dueAfter=not-a-date                       | Invalid value for parameter 'dueAfter'",
                "dueAfter=2050-01-02&dueBefore=2050-01-01  | dueAfter must be before dueBefore",
                "page=-1                                   | Page index must not be negative",
                "page=abc                                  | Invalid value for parameter 'page'",
                "size=0                                    | Page size must be between 1 and 100",
                "size=101                                  | Page size must be between 1 and 100"
        })
        void invalidParameterReturns400ErrorResponse(String queryString, String expectedMessage) throws Exception {
            // Arrange
            String url = BASE_URL + "?" + queryString;

            // Act
            ResultActions result = mockMvc.perform(get(url));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString(expectedMessage)))
                    .andExpect(jsonPath("$.path").value(BASE_URL))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        private MockHttpServletRequestBuilder pagedRequest(int page) {
            return get(BASE_URL)
                    .param("dueAfter", "2045-04-30").param("dueBefore", "2045-06-01")
                    .param("sort", "dueDate,asc")
                    .param("size", "2").param("page", String.valueOf(page));
        }

        private long totalElements(ResultActions result) throws Exception {
            String body = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            return ((Number) JsonPath.read(body, "$.totalElements")).longValue();
        }
    }

    /** Creates a task through the API and returns its id. */
    private String createTask() throws Exception {
        return createTask(TITLE, DUE_DATE);
    }

    /** Creates a task with the given title and due date (possibly null) and returns its id. */
    private String createTask(String title, LocalDate dueDate) throws Exception {
        String body = objectMapper.writeValueAsString(new CreateTaskRequest(title, DESCRIPTION, dueDate));
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

package com.taskmanager.api.infrastructure.web.mapper;

import com.taskmanager.api.domain.exception.InvalidTaskQueryException;
import com.taskmanager.api.domain.model.TaskStatus;
import com.taskmanager.api.domain.query.PageRequest;
import com.taskmanager.api.domain.query.SortDirection;
import com.taskmanager.api.domain.query.TaskQuery;
import com.taskmanager.api.domain.query.TaskSort;
import com.taskmanager.api.domain.query.TaskSortField;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskQueryWebMapperTest {

    @Nested
    class Mapping {

        @Test
        void carriesEveryRawValueIntoTheQuery() {
            // Arrange
            LocalDate after = LocalDate.of(2026, 10, 1);
            LocalDate before = LocalDate.of(2026, 11, 1);

            // Act
            TaskQuery query = TaskQueryWebMapper.toQuery(TaskStatus.IN_PROGRESS, after, before, "title,desc", 3, 50);

            // Assert
            assertThat(query.status()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(query.dueAfter()).isEqualTo(after);
            assertThat(query.dueBefore()).isEqualTo(before);
            assertThat(query.sort()).isEqualTo(new TaskSort(TaskSortField.TITLE, SortDirection.DESC));
            assertThat(query.pageRequest()).isEqualTo(new PageRequest(3, 50));
        }

        @Test
        void noFiltersAndNoSortGivesTheDefaultQuery() {
            // Arrange
            // (all raw values absent, default page and size as the controller supplies them)

            // Act
            TaskQuery query = TaskQueryWebMapper.toQuery(null, null, null, null, 0, 20);

            // Assert
            assertThat(query).isEqualTo(TaskQuery.all());
        }

        @Test
        void propagatesAnInvalidPageFromTheDomain() {
            // Arrange
            Executable act = () -> TaskQueryWebMapper.toQuery(null, null, null, null, -1, 20);

            // Act
            InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

            // Assert
            assertThat(ex.getMessage()).isEqualTo("Page index must not be negative");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 101})
        void propagatesAnInvalidSizeFromTheDomain(int size) {
            // Arrange
            Executable act = () -> TaskQueryWebMapper.toQuery(null, null, null, null, 0, size);

            // Act
            InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

            // Assert
            assertThat(ex.getMessage()).isEqualTo("Page size must be between 1 and 100");
        }

        @Test
        void propagatesAnInvertedDueDateWindowFromTheDomain() {
            // Arrange
            LocalDate day = LocalDate.of(2026, 10, 1);
            Executable act = () -> TaskQueryWebMapper.toQuery(null, day, day, null, 0, 20);

            // Act
            InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

            // Assert
            assertThat(ex.getMessage()).isEqualTo("dueAfter must be before dueBefore");
        }
    }

    @Nested
    class ParsingSort {

        @ParameterizedTest
        @CsvSource({
                "createdAt,asc,   CREATED_AT, ASC",
                "createdAt,desc,  CREATED_AT, DESC",
                "dueDate,asc,     DUE_DATE,   ASC",
                "dueDate,desc,    DUE_DATE,   DESC",
                "title,asc,       TITLE,      ASC",
                "title,desc,      TITLE,      DESC",
                "status,asc,      STATUS,     ASC",
                "status,desc,     STATUS,     DESC"
        })
        void parsesEveryFieldAndDirection(String field, String direction,
                                          TaskSortField expectedField, SortDirection expectedDirection) {
            // Arrange
            String sort = field + "," + direction;

            // Act
            TaskSort parsed = TaskQueryWebMapper.toQuery(null, null, null, sort, 0, 20).sort();

            // Assert
            assertThat(parsed).isEqualTo(new TaskSort(expectedField, expectedDirection));
        }

        @ParameterizedTest
        @ValueSource(strings = {"title,ASC", "title,Asc", "title,aSc"})
        void directionIsCaseInsensitive(String sort) {
            // Arrange
            // (the sort string is the parameter)

            // Act
            TaskSort parsed = TaskQueryWebMapper.toQuery(null, null, null, sort, 0, 20).sort();

            // Assert
            assertThat(parsed).isEqualTo(new TaskSort(TaskSortField.TITLE, SortDirection.ASC));
        }

        @ParameterizedTest
        @CsvSource({
                "createdAt, CREATED_AT",
                "dueDate,   DUE_DATE",
                "title,     TITLE",
                "status,    STATUS"
        })
        void directionDefaultsToAscending(String field, TaskSortField expectedField) {
            // Arrange
            // (a bare field name, no direction)

            // Act
            TaskSort parsed = TaskQueryWebMapper.toQuery(null, null, null, field, 0, 20).sort();

            // Assert
            assertThat(parsed).isEqualTo(new TaskSort(expectedField, SortDirection.ASC));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void absentOrBlankSortUsesTheDefaultSort(String sort) {
            // Arrange
            // (the sort string is the parameter)

            // Act
            TaskSort parsed = TaskQueryWebMapper.toQuery(null, null, null, sort, 0, 20).sort();

            // Assert
            assertThat(parsed).isEqualTo(TaskSort.defaults());
        }

        @Test
        void toleratesSurroundingWhitespace() {
            // Arrange
            String sort = " dueDate , desc ";

            // Act
            TaskSort parsed = TaskQueryWebMapper.toQuery(null, null, null, sort, 0, 20).sort();

            // Assert
            assertThat(parsed).isEqualTo(new TaskSort(TaskSortField.DUE_DATE, SortDirection.DESC));
        }

        @ParameterizedTest
        @ValueSource(strings = {"priority", "created_at", "CREATEDAT", "createdat", "id"})
        void rejectsAnUnknownFieldNamingItAndTheAllowedOnes(String field) {
            // Arrange
            Executable act = () -> TaskQueryWebMapper.toQuery(null, null, null, field + ",asc", 0, 20);

            // Act
            InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

            // Assert
            assertThat(ex.getMessage())
                    .contains("'" + field + "'")
                    .contains("createdAt", "dueDate", "title", "status");
        }

        @ParameterizedTest
        @ValueSource(strings = {"up", "ascending", "descending", "1"})
        void rejectsAnUnknownDirectionNamingItAndTheAllowedOnes(String direction) {
            // Arrange
            Executable act = () -> TaskQueryWebMapper.toQuery(null, null, null, "title," + direction, 0, 20);

            // Act
            InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

            // Assert
            assertThat(ex.getMessage())
                    .contains("'" + direction + "'")
                    .contains("asc", "desc");
        }

        @Test
        void rejectsAnEmptyFieldBeforeTheComma() {
            // Arrange
            Executable act = () -> TaskQueryWebMapper.toQuery(null, null, null, ",asc", 0, 20);

            // Act
            InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("''").contains("createdAt", "dueDate", "title", "status");
        }

        @Test
        void rejectsAnEmptyDirectionAfterTheComma() {
            // Arrange
            Executable act = () -> TaskQueryWebMapper.toQuery(null, null, null, "title,", 0, 20);

            // Act
            InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("''").contains("asc", "desc");
        }

        @ParameterizedTest
        @ValueSource(strings = {"title,asc,extra", "title,asc,", "a,b,c,d"})
        void rejectsMoreThanTwoParts(String sort) {
            // Arrange
            Executable act = () -> TaskQueryWebMapper.toQuery(null, null, null, sort, 0, 20);

            // Act
            InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

            // Assert
            assertThat(ex.getMessage())
                    .contains("'" + sort + "'")
                    .contains("field,direction");
        }
    }
}

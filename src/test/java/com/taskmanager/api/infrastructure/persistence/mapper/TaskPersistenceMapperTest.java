package com.taskmanager.api.infrastructure.persistence.mapper;

import com.taskmanager.api.domain.exception.InvalidTaskException;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.model.TaskStatus;
import com.taskmanager.api.infrastructure.persistence.entity.TaskJpaEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plain unit test for the persistence boundary mapper - no Spring context, no database.
 * {@code Task.reconstruct} validates what it rebuilds, so corrupt persisted data must fail loudly.
 */
class TaskPersistenceMapperTest {

    private static final Clock CREATED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);
    private static final Clock UPDATED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-19T15:30:00Z"), ZoneOffset.UTC);

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 9, 1);

    private static TaskJpaEntity entityWith(UUID id, String title, String description, TaskStatus status,
                                            Instant createdAt, Instant updatedAt) {
        return new TaskJpaEntity(id, title, description, status, DUE_DATE, createdAt, updatedAt);
    }

    private static TaskJpaEntity validEntity() {
        return entityWith(ID, "Persisted title", "Persisted description", TaskStatus.IN_PROGRESS,
                CREATED_CLOCK.instant(), UPDATED_CLOCK.instant());
    }

    @Nested
    class RoundTrip {

        @Test
        void toEntityCopiesEveryFieldOfTheTask() {
            // Arrange
            Task task = Task.reconstruct(ID, "Write tests", "Follow TDD", TaskStatus.IN_PROGRESS,
                    DUE_DATE, CREATED_CLOCK.instant(), UPDATED_CLOCK.instant());

            // Act
            TaskJpaEntity entity = TaskPersistenceMapper.toEntity(task);

            // Assert
            assertThat(entity.getId()).isEqualTo(ID);
            assertThat(entity.getTitle()).isEqualTo("Write tests");
            assertThat(entity.getDescription()).isEqualTo("Follow TDD");
            assertThat(entity.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(entity.getDueDate()).isEqualTo(DUE_DATE);
            assertThat(entity.getCreatedAt()).isEqualTo(CREATED_CLOCK.instant());
            assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_CLOCK.instant());
        }

        @Test
        void toDomainCopiesEveryFieldOfTheEntity() {
            // Arrange
            TaskJpaEntity entity = validEntity();

            // Act
            Task task = TaskPersistenceMapper.toDomain(entity);

            // Assert
            assertThat(task.id()).isEqualTo(ID);
            assertThat(task.title()).isEqualTo("Persisted title");
            assertThat(task.description()).isEqualTo("Persisted description");
            assertThat(task.status()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(task.dueDate()).isEqualTo(DUE_DATE);
            assertThat(task.createdAt()).isEqualTo(CREATED_CLOCK.instant());
            assertThat(task.updatedAt()).isEqualTo(UPDATED_CLOCK.instant());
        }

        @Test
        void toEntityThenToDomainRoundTripsEveryField() {
            // Arrange
            Task original = Task.reconstruct(ID, "Round trip", "Every field survives", TaskStatus.DONE,
                    DUE_DATE, CREATED_CLOCK.instant(), UPDATED_CLOCK.instant());

            // Act
            Task roundTripped = TaskPersistenceMapper.toDomain(TaskPersistenceMapper.toEntity(original));

            // Assert (Task.equals compares id only, so every field is checked explicitly)
            assertThat(roundTripped.id()).isEqualTo(original.id());
            assertThat(roundTripped.title()).isEqualTo(original.title());
            assertThat(roundTripped.description()).isEqualTo(original.description());
            assertThat(roundTripped.status()).isEqualTo(original.status());
            assertThat(roundTripped.dueDate()).isEqualTo(original.dueDate());
            assertThat(roundTripped.createdAt()).isEqualTo(original.createdAt());
            assertThat(roundTripped.updatedAt()).isEqualTo(original.updatedAt());
        }

        @Test
        void roundTripPreservesNullDescriptionAndDueDate() {
            // Arrange
            Task original = Task.create("Title only", null, null, CREATED_CLOCK);

            // Act
            Task roundTripped = TaskPersistenceMapper.toDomain(TaskPersistenceMapper.toEntity(original));

            // Assert
            assertThat(roundTripped.id()).isEqualTo(original.id());
            assertThat(roundTripped.description()).isNull();
            assertThat(roundTripped.dueDate()).isNull();
            assertThat(roundTripped.status()).isEqualTo(TaskStatus.TODO);
            assertThat(roundTripped.createdAt()).isEqualTo(CREATED_CLOCK.instant());
            assertThat(roundTripped.updatedAt()).isEqualTo(CREATED_CLOCK.instant());
        }

        @ParameterizedTest
        @ValueSource(strings = {"TODO", "IN_PROGRESS", "DONE"})
        void roundTripPreservesEveryStatus(String statusName) {
            // Arrange
            TaskStatus status = TaskStatus.valueOf(statusName);
            Task original = Task.reconstruct(ID, "Status check", null, status,
                    null, CREATED_CLOCK.instant(), UPDATED_CLOCK.instant());

            // Act
            Task roundTripped = TaskPersistenceMapper.toDomain(TaskPersistenceMapper.toEntity(original));

            // Assert
            assertThat(roundTripped.status()).isEqualTo(status);
        }
    }

    @Nested
    class CorruptPersistedData {

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   \t\n"})
        void toDomainFailsWhenPersistedTitleIsBlank(String blankTitle) {
            // Arrange
            TaskJpaEntity entity = entityWith(ID, blankTitle, "desc", TaskStatus.TODO,
                    CREATED_CLOCK.instant(), UPDATED_CLOCK.instant());
            Executable act = () -> TaskPersistenceMapper.toDomain(entity);

            // Act
            InvalidTaskException ex = assertThrows(InvalidTaskException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("title").contains("blank");
        }

        @Test
        void toDomainFailsWhenPersistedTitleIsNull() {
            // Arrange
            TaskJpaEntity entity = entityWith(ID, null, "desc", TaskStatus.TODO,
                    CREATED_CLOCK.instant(), UPDATED_CLOCK.instant());
            Executable act = () -> TaskPersistenceMapper.toDomain(entity);

            // Act
            InvalidTaskException ex = assertThrows(InvalidTaskException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("title");
        }

        static Stream<Arguments> entitiesWithNullRequiredField() {
            Instant created = CREATED_CLOCK.instant();
            Instant updated = UPDATED_CLOCK.instant();
            return Stream.of(
                    Arguments.of("id", entityWith(null, "Title", "desc", TaskStatus.TODO, created, updated)),
                    Arguments.of("status", entityWith(ID, "Title", "desc", null, created, updated)),
                    Arguments.of("createdAt", entityWith(ID, "Title", "desc", TaskStatus.TODO, null, updated)),
                    Arguments.of("updatedAt", entityWith(ID, "Title", "desc", TaskStatus.TODO, created, null))
            );
        }

        @ParameterizedTest(name = "persisted null {0}")
        @MethodSource("entitiesWithNullRequiredField")
        void toDomainFailsWhenPersistedRequiredFieldIsNull(String field, TaskJpaEntity entity) {
            // Arrange
            Executable act = () -> TaskPersistenceMapper.toDomain(entity);

            // Act
            NullPointerException ex = assertThrows(NullPointerException.class, act);

            // Assert
            assertThat(ex.getMessage()).isEqualTo(field + " must not be null");
        }

        @Test
        void toDomainFailsWhenPersistedTitleExceeds200Characters() {
            // Arrange
            TaskJpaEntity entity = entityWith(ID, "a".repeat(201), "desc", TaskStatus.TODO,
                    CREATED_CLOCK.instant(), UPDATED_CLOCK.instant());
            Executable act = () -> TaskPersistenceMapper.toDomain(entity);

            // Act
            InvalidTaskException ex = assertThrows(InvalidTaskException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("title").contains("200");
        }

        @Test
        void toDomainFailsWhenPersistedDescriptionExceeds2000Characters() {
            // Arrange
            TaskJpaEntity entity = entityWith(ID, "Title", "d".repeat(2001), TaskStatus.TODO,
                    CREATED_CLOCK.instant(), UPDATED_CLOCK.instant());
            Executable act = () -> TaskPersistenceMapper.toDomain(entity);

            // Act
            InvalidTaskException ex = assertThrows(InvalidTaskException.class, act);

            // Assert
            assertThat(ex.getMessage()).contains("description").contains("2000");
        }

        @Test
        void toDomainAcceptsTitleOfExactly200Characters() {
            // Arrange
            String title = "a".repeat(200);
            TaskJpaEntity entity = entityWith(ID, title, "desc", TaskStatus.TODO,
                    CREATED_CLOCK.instant(), UPDATED_CLOCK.instant());

            // Act
            Task task = assertDoesNotThrow(() -> TaskPersistenceMapper.toDomain(entity));

            // Assert
            assertThat(task.title()).isEqualTo(title);
        }

        @Test
        void toDomainAcceptsDescriptionOfExactly2000Characters() {
            // Arrange
            String description = "d".repeat(2000);
            TaskJpaEntity entity = entityWith(ID, "Title", description, TaskStatus.TODO,
                    CREATED_CLOCK.instant(), UPDATED_CLOCK.instant());

            // Act
            Task task = assertDoesNotThrow(() -> TaskPersistenceMapper.toDomain(entity));

            // Assert
            assertThat(task.description()).isEqualTo(description);
        }
    }
}

package com.taskmanager.api.application.usecase;

import com.taskmanager.api.application.command.CreateTaskCommand;
import com.taskmanager.api.domain.exception.InvalidTaskException;
import com.taskmanager.api.domain.model.Task;
import com.taskmanager.api.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTaskServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TaskRepository taskRepository;

    private CreateTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateTaskService(taskRepository, FIXED_CLOCK);
    }

    @Test
    void createsTaskAndPersistsIt() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = useCase.execute(new CreateTaskCommand("Write tests", "Follow TDD", LocalDate.of(2026, 9, 1)));

        assertThat(result.title()).isEqualTo("Write tests");
        assertThat(result.description()).isEqualTo("Follow TDD");
        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 9, 1));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().title()).isEqualTo("Write tests");
    }

    @Test
    void propagatesDomainValidationFailure() {
        assertThatThrownBy(() -> useCase.execute(new CreateTaskCommand("", "desc", null)))
                .isInstanceOf(InvalidTaskException.class);
    }
}

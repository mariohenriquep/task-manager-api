package com.taskmanager.api.domain.query;

import com.taskmanager.api.domain.exception.InvalidTaskQueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageRequestTest {

    @Test
    void defaultsAreFirstPageOfTwentyElements() {
        // Arrange
        // (no inputs: defaults() takes none)

        // Act
        PageRequest defaults = PageRequest.defaults();

        // Assert
        assertThat(defaults.page()).isZero();
        assertThat(defaults.size()).isEqualTo(20);
    }

    @Test
    void exposesSizeConstants() {
        // Arrange
        // (constants are read directly)

        // Act
        int defaultSize = PageRequest.DEFAULT_SIZE;
        int maxSize = PageRequest.MAX_SIZE;

        // Assert
        assertThat(defaultSize).isEqualTo(20);
        assertThat(maxSize).isEqualTo(100);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5000})
    void acceptsNonNegativePage(int page) {
        // Arrange
        int size = 10;

        // Act
        PageRequest request = new PageRequest(page, size);

        // Assert
        assertThat(request.page()).isEqualTo(page);
        assertThat(request.size()).isEqualTo(size);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
    void rejectsNegativePage(int page) {
        // Arrange
        Executable act = () -> new PageRequest(page, 10);

        // Act
        InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

        // Assert
        assertThat(ex.getMessage()).contains("Page index").contains("negative");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 20, 99, 100})
    void acceptsSizeWithinBounds(int size) {
        // Arrange
        int page = 0;

        // Act
        PageRequest request = new PageRequest(page, size);

        // Assert
        assertThat(request.size()).isEqualTo(size);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, 1000, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void rejectsSizeOutsideBounds(int size) {
        // Arrange
        Executable act = () -> new PageRequest(0, size);

        // Act
        InvalidTaskQueryException ex = assertThrows(InvalidTaskQueryException.class, act);

        // Assert
        assertThat(ex.getMessage()).contains("Page size").contains("between 1 and 100");
    }
}

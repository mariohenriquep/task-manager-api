package com.taskmanager.api.domain.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageTest {

    @Test
    void holdsContentAndPagingMetadata() {
        // Arrange
        List<String> content = List.of("a", "b");

        // Act
        Page<String> page = new Page<>(content, 1, 2, 5);

        // Assert
        assertThat(page.content()).containsExactly("a", "b");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(5);
    }

    @Test
    void takesDefensiveCopyOfContent() {
        // Arrange
        List<String> source = new ArrayList<>(List.of("a", "b"));
        Page<String> page = new Page<>(source, 0, 10, 2);

        // Act
        source.add("c");

        // Assert
        assertThat(page.content()).containsExactly("a", "b");
    }

    @Test
    void contentIsUnmodifiable() {
        // Arrange
        Page<String> page = new Page<>(List.of("a"), 0, 10, 1);
        Executable act = () -> page.content().add("b");

        // Act
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, act);

        // Assert
        assertThat(ex).isNotNull();
        assertThat(page.content()).containsExactly("a");
    }

    @Test
    void rejectsNullContent() {
        // Arrange
        Executable act = () -> new Page<String>(null, 0, 10, 0);

        // Act
        NullPointerException ex = assertThrows(NullPointerException.class, act);

        // Assert
        assertThat(ex.getMessage()).contains("content");
    }

    @Test
    void emptyPageHasZeroTotalPages() {
        // Arrange
        Page<String> page = new Page<>(List.of(), 0, 20, 0);

        // Act
        int totalPages = page.totalPages();

        // Assert
        assertThat(totalPages).isZero();
    }

    @ParameterizedTest(name = "{0} elements, size {1} -> {2} pages")
    @CsvSource({
            "1,   20, 1",
            "20,  20, 1",
            "21,  20, 2",
            "40,  20, 2",
            "45,  20, 3",
            "5,   1,  5",
            "1,   1,  1",
            "100, 100, 1",
            "101, 100, 2"
    })
    void totalPagesIsCeilingOfTotalElementsOverSize(long totalElements, int size, int expected) {
        // Arrange
        Page<String> page = new Page<>(List.of(), 0, size, totalElements);

        // Act
        int totalPages = page.totalPages();

        // Assert
        assertThat(totalPages).isEqualTo(expected);
    }

    @Test
    void totalPagesDoesNotOverflowForVeryLargeTotals() {
        // Arrange
        Page<String> page = new Page<>(List.of(), 0, 1, Integer.MAX_VALUE);

        // Act
        int totalPages = page.totalPages();

        // Assert
        assertThat(totalPages).isEqualTo(Integer.MAX_VALUE);
    }

    @ParameterizedTest(name = "page={0}, size={1}, totalElements={2} is rejected: {3}")
    @CsvSource({
            "-1, 20, 0, page",
            "0, 0, 0, size",
            "0, -5, 0, size",
            "0, 20, -1, totalElements"
    })
    void rejectsImpossibleValues(int page, int size, long totalElements, String offendingField) {
        // Arrange
        Executable act = () -> new Page<String>(List.of(), page, size, totalElements);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, act);

        // Assert
        assertThat(ex.getMessage()).contains(offendingField);
    }

    @Test
    void acceptsAnEmptyFirstPageWithSizeOne() {
        // Arrange
        List<String> noContent = List.of();

        // Act
        Page<String> result = new Page<>(noContent, 0, 1, 0);

        // Assert
        assertThat(result.content()).isEmpty();
        assertThat(result.totalPages()).isZero();
    }
}

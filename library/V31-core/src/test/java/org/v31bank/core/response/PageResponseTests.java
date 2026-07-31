package org.v31bank.core.response;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PageResponse}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class PageResponseTests {

    @Test
    void countsPagesFromTheTotal() {
        PageResponse<String> page = PageResponse.of(List.of("a", "b"), 21, 1, 10);
        assertEquals(3, page.totalPages());
        assertTrue(page.hasNext());
    }

    @Test
    void reportsNoNextPageOnTheLastOne() {
        PageResponse<String> page = PageResponse.of(List.of("a"), 21, 3, 10);
        assertEquals(3, page.totalPages());
        assertFalse(page.hasNext());
    }

    @Test
    void recomputesDerivedMembersRatherThanTrustingThem() {
        PageResponse<String> page = new PageResponse<>(List.of("a"), 21, 1, 10, 99, false);
        assertEquals(3, page.totalPages());
        assertTrue(page.hasNext());
    }

    @Test
    void survivesAPageSizeOfZero() {
        PageResponse<String> page = PageResponse.of(List.of(), 0, 1, 0);
        assertEquals(0, page.totalPages());
        assertFalse(page.hasNext());
    }

    @Test
    void emptyKeepsThePageThatWasAskedFor() {
        PageResponse<String> page = PageResponse.empty(2, 25);
        assertEquals(List.of(), page.records());
        assertEquals(0, page.total());
        assertEquals(2, page.pageNumber());
        assertEquals(25, page.pageSize());
        assertFalse(page.hasNext());
    }

    @Test
    void mapConvertsTheRecordsAndKeepsThePagination() {
        PageResponse<Integer> lengths = PageResponse.of(List.of("aa", "bbb"), 21, 1, 10).map(String::length);
        assertEquals(List.of(2, 3), lengths.records());
        assertEquals(21, lengths.total());
        assertEquals(3, lengths.totalPages());
        assertTrue(lengths.hasNext());
    }

    @Test
    void recordsAreCopiedAndUnmodifiable() {
        List<String> records = new ArrayList<>(List.of("a"));
        PageResponse<String> page = PageResponse.of(records, 1, 1, 10);
        records.clear();
        assertEquals(List.of("a"), page.records());
        assertThrows(UnsupportedOperationException.class, () -> page.records().add("b"));
    }

    @Test
    void treatsMissingRecordsAsAnEmptyPage() {
        assertEquals(List.of(), new PageResponse<>(null, 0, 1, 10, 0, false).records());
    }

}

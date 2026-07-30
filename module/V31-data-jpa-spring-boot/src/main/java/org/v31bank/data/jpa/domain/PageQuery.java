package org.v31bank.data.jpa.domain;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Base class for paginated query requests, using one-based page numbering.
 * <p>
 * Query objects are expected to extend this class with their own filter fields
 * and convert to a Spring Data {@link Pageable} via {@link #toPageable()}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class PageQuery {

    /**
     * The number of the first page.
     */
    public static final int FIRST_PAGE_NUMBER = 1;

    /**
     * The page size applied when none is specified.
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * The maximum page size a caller may request.
     */
    public static final int MAX_PAGE_SIZE = 500;

    /**
     * One-based page number.
     */
    private int pageNumber = FIRST_PAGE_NUMBER;

    private int pageSize = DEFAULT_PAGE_SIZE;

    public int getPageNumber() {
        return this.pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Convert this query to an unsorted {@link Pageable}.
     * @return the pageable
     */
    public Pageable toPageable() {
        return toPageable(Sort.unsorted());
    }

    /**
     * Convert this query to a {@link Pageable} with the given sort, translating the
     * one-based page number to Spring Data's zero-based index and clamping the page
     * size to {@code [1, MAX_PAGE_SIZE]}.
     * @param sort the sort to apply
     * @return the pageable
     */
    public Pageable toPageable(Sort sort) {
        int number = Math.max(this.pageNumber, FIRST_PAGE_NUMBER) - 1;
        int size = Math.clamp(this.pageSize, 1, MAX_PAGE_SIZE);
        return PageRequest.of(number, size, sort);
    }

}

package org.v31bank.data.jpa.domain;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * A page of results returned to callers, decoupling the API surface from Spring
 * Data's {@link Page} and using one-based page numbering.
 *
 * @param <T> the type of the page content
 */
public class PageResult<T> {

    private final List<T> records;

    private final long total;

    /**
     * One-based page number.
     */
    private final int pageNumber;

    private final int pageSize;

    private PageResult(List<T> records, long total, int pageNumber, int pageSize) {
        this.records = records;
        this.total = total;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    /**
     * Create a result from a Spring Data {@link Page}, translating its zero-based
     * page index to a one-based page number.
     * @param page the page to convert
     * @param <T> the type of the page content
     * @return the converted result
     */
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getTotalElements(), page.getNumber() + 1, page.getSize());
    }

    /**
     * Create a result from an already paginated list.
     * @param records the content of the current page
     * @param total the total number of matching records
     * @param pageNumber the one-based page number
     * @param pageSize the page size
     * @param <T> the type of the page content
     * @return the result
     */
    public static <T> PageResult<T> of(List<T> records, long total, int pageNumber, int pageSize) {
        return new PageResult<>(List.copyOf(records), total, pageNumber, pageSize);
    }

    /**
     * Create an empty result for the given query.
     * @param query the originating query
     * @param <T> the type of the page content
     * @return the empty result
     */
    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(List.of(), 0, query.getPageNumber(), query.getPageSize());
    }

    /**
     * Return a new result with each record converted by the given function,
     * keeping the pagination information.
     * @param converter the conversion to apply to each record
     * @param <R> the target record type
     * @return the converted result
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> converter) {
        return new PageResult<>(this.records.stream().<R>map(converter).toList(), this.total, this.pageNumber,
                this.pageSize);
    }

    public List<T> getRecords() {
        return this.records;
    }

    public long getTotal() {
        return this.total;
    }

    public int getPageNumber() {
        return this.pageNumber;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public int getTotalPages() {
        return (this.pageSize == 0) ? 0 : (int) Math.ceilDiv(this.total, this.pageSize);
    }

    public boolean hasNext() {
        return this.pageNumber < getTotalPages();
    }

}

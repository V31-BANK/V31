package org.v31bank.jooq.util;

import java.util.Objects;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectLimitStep;

import org.v31bank.core.response.PageResponse;

/**
 * Runs a jOOQ query one page at a time.
 * <p>
 * jOOQ has no equivalent of Spring Data's repository pagination, so the count
 * query and the windowed query are written out here rather than in every service
 * that needs them — and written out once, since the two are easy to let drift
 * into disagreeing about which rows they describe.
 * <p>
 * The query passed in must carry its own filtering and ordering but no limit:
 * both the count and the page are derived from it, so a query that already
 * limits itself would report a total it does not have. Ordering matters more
 * than it looks — without it a database is free to return rows in any order, and
 * a row can appear on two consecutive pages or on neither.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class JooqPages {

    /**
     * The number of the first page, one-based to match
     * {@link PageResponse#FIRST_PAGE_NUMBER}.
     */
    public static final int FIRST_PAGE_NUMBER = PageResponse.FIRST_PAGE_NUMBER;

    /**
     * The largest page a caller may ask for. A caller asking for more is given
     * this rather than an error, because the alternative — honouring it — lets
     * one request pull a whole table into memory.
     */
    public static final int MAX_PAGE_SIZE = 500;

    private JooqPages() {
    }

    /**
     * Count the rows the query matches and fetch the page asked for.
     * <p>
     * The count runs first and the page is skipped entirely when nothing matched,
     * which is the common case for a filtered search and saves the second round
     * trip.
     * @param dsl the context to run the count with
     * @param query the query to page over, filtered and ordered but not limited
     * @param pageNumber the one-based page to fetch, raised to the first page
     * when lower
     * @param pageSize the page size, clamped to {@code [1, MAX_PAGE_SIZE]}
     * @param <R> the record type the query returns
     * @return the page, carrying the total across all pages
     */
    public static <R extends Record> PageResponse<R> fetch(DSLContext dsl, SelectLimitStep<R> query, int pageNumber,
            int pageSize) {
        Objects.requireNonNull(dsl, "dsl must not be null");
        Objects.requireNonNull(query, "query must not be null");
        int number = Math.max(pageNumber, FIRST_PAGE_NUMBER);
        int size = Math.clamp(pageSize, 1, MAX_PAGE_SIZE);
        long total = dsl.fetchCountLarge(query);
        if (total == 0) {
            return PageResponse.empty(number, size);
        }
        Result<R> records = query.offset((number - 1) * (long) size).limit(size).fetch();
        return PageResponse.of(records, total, number, size);
    }

}

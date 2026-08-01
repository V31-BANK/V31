package org.v31bank.cbs.application.dto;

import org.v31bank.cbs.domain.constant.BankProductCategory;
import org.v31bank.cbs.domain.constant.BankProductStatus;

/**
 * Paginated bank product query, using one-based page numbering.
 * <p>
 * The filters are limited to what the store can answer without reading
 * everything. Valkey has no query planner: a page comes from a sorted set kept
 * up to date as products are written, and the only filters offered are the ones
 * that have such a set behind them. A substring search over names would mean
 * walking every key on the instance, which is why it is not offered rather than
 * offered slowly.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class BankProductPageQuery {

    /**
     * The number of the first page.
     */
    public static final int FIRST_PAGE_NUMBER = 1;

    /**
     * The page size applied when none is specified.
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * One-based page number.
     */
    private int pageNumber = FIRST_PAGE_NUMBER;

    private int pageSize = DEFAULT_PAGE_SIZE;

    /**
     * Category to match.
     */
    private BankProductCategory category;

    /**
     * Status to match.
     */
    private BankProductStatus status;

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

    public BankProductCategory getCategory() {
        return this.category;
    }

    public void setCategory(BankProductCategory category) {
        this.category = category;
    }

    public BankProductStatus getStatus() {
        return this.status;
    }

    public void setStatus(BankProductStatus status) {
        this.status = status;
    }

}

package org.v31bank.compliance.application.dto;

import java.util.UUID;

import org.v31bank.compliance.domain.constant.ComplianceCaseStatus;
import org.v31bank.compliance.domain.constant.ComplianceCaseType;

/**
 * Paginated compliance case query with optional filters, using one-based page
 * numbering.
 * <p>
 * The JPA services extend {@code org.v31bank.data.jpa.domain.PageQuery}, which
 * converts itself into a Spring Data {@code Pageable}. This service has no Spring
 * Data on its classpath, so the pagination request is carried here and handed to
 * {@code JooqPages}, which clamps it and turns it into a limit and an offset.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ComplianceCasePageQuery {

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
     * Case number fragment to match, case-insensitive.
     */
    private String caseNumber;

    /**
     * Return only the cases opened against this customer.
     */
    private UUID customerId;

    /**
     * Type to match.
     */
    private ComplianceCaseType type;

    /**
     * Status to match.
     */
    private ComplianceCaseStatus status;

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

    public String getCaseNumber() {
        return this.caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public UUID getCustomerId() {
        return this.customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public ComplianceCaseType getType() {
        return this.type;
    }

    public void setType(ComplianceCaseType type) {
        this.type = type;
    }

    public ComplianceCaseStatus getStatus() {
        return this.status;
    }

    public void setStatus(ComplianceCaseStatus status) {
        this.status = status;
    }

}

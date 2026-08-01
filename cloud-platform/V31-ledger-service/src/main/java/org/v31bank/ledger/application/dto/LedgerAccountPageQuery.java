package org.v31bank.ledger.application.dto;

import org.v31bank.ledger.domain.constant.LedgerAccountType;
import org.v31bank.ledger.domain.constant.LedgerAccountStatus;
import org.v31bank.data.jpa.domain.PageQuery;

/**
 * Paginated ledger account query with optional filters.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class LedgerAccountPageQuery extends PageQuery {

    /**
     * Code fragment to match, case-insensitive.
     */
    private String code;

    /**
     * Type to match.
     */
    private LedgerAccountType type;

    /**
     * Status to match.
     */
    private LedgerAccountStatus status;

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LedgerAccountType getType() {
        return this.type;
    }

    public void setType(LedgerAccountType type) {
        this.type = type;
    }

    public LedgerAccountStatus getStatus() {
        return this.status;
    }

    public void setStatus(LedgerAccountStatus status) {
        this.status = status;
    }

}

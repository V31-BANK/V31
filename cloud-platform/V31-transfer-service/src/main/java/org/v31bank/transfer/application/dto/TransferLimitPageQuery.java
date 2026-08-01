package org.v31bank.transfer.application.dto;

import java.math.BigDecimal;
import org.v31bank.transfer.domain.constant.TransferLimitStatus;
import org.v31bank.data.jpa.domain.PageQuery;

/**
 * Paginated transfer limit query with optional filters.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class TransferLimitPageQuery extends PageQuery {

    /**
     * Code fragment to match, case-insensitive.
     */
    private String code;

    /**
     * Status to match.
     */
    private TransferLimitStatus status;

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public TransferLimitStatus getStatus() {
        return this.status;
    }

    public void setStatus(TransferLimitStatus status) {
        this.status = status;
    }

}

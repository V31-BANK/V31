package org.v31bank.customer.application.dto;

import org.v31bank.customer.domain.constant.CustomerStatus;
import org.v31bank.data.jpa.domain.PageQuery;

/**
 * Paginated customer query with optional filters.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class CustomerPageQuery extends PageQuery {

    /**
     * Email fragment to match, case-insensitive.
     */
    private String email;

    /**
     * Status to match.
     */
    private CustomerStatus status;

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CustomerStatus getStatus() {
        return this.status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

}

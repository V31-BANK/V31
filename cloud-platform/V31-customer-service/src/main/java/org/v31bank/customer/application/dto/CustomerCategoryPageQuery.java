package org.v31bank.customer.application.dto;

import java.util.UUID;

import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.data.jpa.domain.PageQuery;

/**
 * Paginated customer category query with optional filters.
 * <p>
 * The result is a flat page across the whole hierarchy unless {@link #parentId}
 * or {@link #rootOnly} narrows it to a single level.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class CustomerCategoryPageQuery extends PageQuery {

    /**
     * Code fragment to match, case-insensitive.
     */
    private String code;

    /**
     * Name fragment to match, case-insensitive.
     */
    private String name;

    /**
     * Status to match.
     */
    private CustomerCategoryStatus status;

    /**
     * Return only the direct children of this node. Ignored when
     * {@link #rootOnly} is set.
     */
    private UUID parentId;

    /**
     * Return only root nodes. Takes precedence over {@link #parentId}, since a
     * {@code null} parent identifier already means "any parent".
     */
    private boolean rootOnly;

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CustomerCategoryStatus getStatus() {
        return this.status;
    }

    public void setStatus(CustomerCategoryStatus status) {
        this.status = status;
    }

    public UUID getParentId() {
        return this.parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public boolean isRootOnly() {
        return this.rootOnly;
    }

    public void setRootOnly(boolean rootOnly) {
        this.rootOnly = rootOnly;
    }

}

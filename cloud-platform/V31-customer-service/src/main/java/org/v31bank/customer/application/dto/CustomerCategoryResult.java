package org.v31bank.customer.application.dto;

import java.util.UUID;

import org.v31bank.customer.domain.model.CustomerCategory;

/**
 * Outcome of a customer category command.
 * <p>
 * A tree mutation fails for several distinct reasons that a plain
 * {@link java.util.Optional} cannot tell apart, so commands report the outcome
 * explicitly and leave it to the caller to translate it into a
 * transport-specific form.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public sealed interface CustomerCategoryResult {

    /**
     * The command succeeded.
     *
     * @param category the affected category
     */
    record Success(CustomerCategory category) implements CustomerCategoryResult {
    }

    /**
     * No category exists with the given identifier.
     *
     * @param id the identifier that was looked up
     */
    record NotFound(UUID id) implements CustomerCategoryResult {
    }

    /**
     * The requested parent does not exist.
     *
     * @param parentId the identifier that was looked up
     */
    record ParentNotFound(UUID parentId) implements CustomerCategoryResult {
    }

    /**
     * Another category already uses the requested code.
     *
     * @param code the conflicting code
     */
    record DuplicateCode(String code) implements CustomerCategoryResult {
    }

    /**
     * The requested parent would make the category an ancestor of itself.
     *
     * @param id the category being moved
     * @param parentId the rejected parent
     */
    record CyclicParent(UUID id, UUID parentId) implements CustomerCategoryResult {
    }

    /**
     * The category still has children, so removing it would orphan them.
     *
     * @param id the category that was to be deleted
     */
    record HasChildren(UUID id) implements CustomerCategoryResult {
    }

}

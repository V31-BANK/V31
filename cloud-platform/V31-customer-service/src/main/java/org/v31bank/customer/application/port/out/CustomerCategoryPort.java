package org.v31bank.customer.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.v31bank.customer.application.dto.CustomerCategoryPageQuery;
import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.customer.domain.model.CustomerCategory;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Output port for {@link CustomerCategory} persistence, implemented by the
 * infrastructure layer.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface CustomerCategoryPort {

    CustomerCategory save(CustomerCategory category);

    Optional<CustomerCategory> findById(UUID id);

    /**
     * Find every category as a flat list, ordered by sibling position.
     * @param status status to match, or {@code null} for no filter
     * @return the matching categories, ready to be assembled into a tree
     */
    List<CustomerCategory> findAll(CustomerCategoryStatus status);

    /**
     * Find a page of categories matching the filters carried by the query.
     * @param query the filters and the pagination request
     * @return the page of matching categories
     */
    PageResult<CustomerCategory> findPage(CustomerCategoryPageQuery query);

    /**
     * Whether any category is a direct child of the given node.
     * @param parentId the parent identifier
     * @return {@code true} if the node has at least one child
     */
    boolean existsByParentId(UUID parentId);

    /**
     * Whether any category already uses the given code.
     * @param code the code to check
     * @return {@code true} if the code is taken
     */
    boolean existsByCode(String code);

    void delete(CustomerCategory category);

}

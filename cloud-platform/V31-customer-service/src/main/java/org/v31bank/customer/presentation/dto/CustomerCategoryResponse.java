package org.v31bank.customer.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.customer.domain.model.CustomerCategory;

/**
 * API representation of a customer category.
 *
 * @param children the descendants, empty for the flat representation returned by
 * the paginated and single-node endpoints
 * @author Xander Wang
 * @since 0.2.0
 */
public record CustomerCategoryResponse(UUID id, UUID parentId, String code, String name, Integer sortOrder,
        CustomerCategoryStatus status, Instant createdDate, Instant lastModifiedDate,
        List<CustomerCategoryResponse> children) {

    /**
     * Represent a single node, without its descendants.
     * @param category the category to represent
     * @return the flat representation
     */
    public static CustomerCategoryResponse from(CustomerCategory category) {
        return of(category, List.of());
    }

    /**
     * Represent a node together with the subtree already attached to it.
     * @param category the root of an assembled subtree
     * @return the nested representation
     */
    public static CustomerCategoryResponse fromTree(CustomerCategory category) {
        return of(category, category.getChildren().stream().map(CustomerCategoryResponse::fromTree).toList());
    }

    private static CustomerCategoryResponse of(CustomerCategory category, List<CustomerCategoryResponse> children) {
        return new CustomerCategoryResponse(category.getId(), category.getParentId(), category.getCode(),
                category.getName(), category.getSortOrder(), category.getStatus(), category.getCreatedDate(),
                category.getLastModifiedDate(), children);
    }

}

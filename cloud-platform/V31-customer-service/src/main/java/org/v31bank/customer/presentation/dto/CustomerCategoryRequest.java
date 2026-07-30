package org.v31bank.customer.presentation.dto;

import java.util.UUID;

import org.v31bank.customer.domain.constant.CustomerCategoryStatus;

/**
 * Request body for creating or updating a customer category.
 *
 * @param code the unique business code
 * @param name the display name
 * @param parentId the parent node, or {@code null} for a root category
 * @param sortOrder the position among siblings, or {@code null}
 * @param status the status; defaults to {@code ENABLED} on create and is left
 * unchanged on update when {@code null}
 * @author Xander Wang
 * @since 0.2.0
 */
public record CustomerCategoryRequest(String code, String name, UUID parentId, Integer sortOrder,
        CustomerCategoryStatus status) {

}

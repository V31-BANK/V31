/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.v31bank.customer.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.customer.domain.model.CustomerCategory;

/**
 * API representation of a customer category.
 *
 * @param children the descendants, empty for the flat representation returned by the
 * paginated and single-node endpoints
 * @param id the identifier it was issued when it was created
 * @param parentId the category this one sits under, or {@code null} at the top
 * @param code the code it is known by, unique within the service
 * @param name what to call it
 * @param sortOrder where it belongs among its siblings
 * @param status where it is in its lifecycle
 * @param createdDate when it was created
 * @param lastModifiedDate when it was last changed
 * @param children the categories underneath it, deepest last
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

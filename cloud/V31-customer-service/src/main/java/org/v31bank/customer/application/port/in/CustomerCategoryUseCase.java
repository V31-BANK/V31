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

package org.v31bank.customer.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.v31bank.core.response.ApiResponse;
import org.v31bank.customer.application.dto.CustomerCategoryPageQuery;
import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.customer.domain.model.CustomerCategory;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Use cases for managing the customer category hierarchy.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface CustomerCategoryUseCase {

	/**
	 * Create a category, optionally under an existing parent.
	 * @param code the unique business code
	 * @param name the display name
	 * @param parentId the parent node, or {@code null} to create a root
	 * @param sortOrder the position among siblings, or {@code null}
	 * @param status the initial status, or {@code null} for the default
	 * @return the outcome of the command
	 */
	ApiResponse<CustomerCategory> create(String code, String name, UUID parentId, Integer sortOrder,
			CustomerCategoryStatus status);

	Optional<CustomerCategory> get(UUID id);

	/**
	 * Find a flat page of categories, without assembling them into a tree.
	 * @param query the filters and the pagination request
	 * @return the page of matching categories
	 */
	PageResult<CustomerCategory> page(CustomerCategoryPageQuery query);

	/**
	 * Assemble the hierarchy into a tree, with each node carrying its children.
	 * @param rootId the subtree to return, or {@code null} for every root
	 * @param status status to match, or {@code null} for no filter
	 * @return the root nodes, with descendants attached, or an empty list if
	 * {@code rootId} matches no node
	 */
	List<CustomerCategory> tree(UUID rootId, CustomerCategoryStatus status);

	/**
	 * Update the category with the given identifier, moving it in the hierarchy when
	 * {@code parentId} changes.
	 * @param id the category to update
	 * @param code the unique business code
	 * @param name the display name
	 * @param parentId the new parent, or {@code null} to make it a root
	 * @param sortOrder the position among siblings, or {@code null}
	 * @param status the new status, or {@code null} to leave it unchanged
	 * @return the outcome of the command
	 */
	ApiResponse<CustomerCategory> update(UUID id, String code, String name, UUID parentId, Integer sortOrder,
			CustomerCategoryStatus status);

	/**
	 * Delete the category with the given identifier, provided it is a leaf.
	 * @param id the category to delete
	 * @return the outcome of the command
	 */
	ApiResponse<CustomerCategory> delete(UUID id);

}

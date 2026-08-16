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

package org.v31bank.customer.application.dto;

import java.util.UUID;

import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.data.jpa.domain.PageQuery;

/**
 * Paginated customer category query with optional filters.
 * <p>
 * The result is a flat page across the whole hierarchy unless {@link #parentId} or
 * {@link #rootOnly} narrows it to a single level.
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
	 * Return only the direct children of this node. Ignored when {@link #rootOnly} is
	 * set.
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

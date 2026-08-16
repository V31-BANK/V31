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

package org.v31bank.customer.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.data.jpa.domain.TreeEntity;

/**
 * A node in the customer category hierarchy, for example
 * {@code Retail > High Net Worth > Private Banking}.
 * <p>
 * The parent reference and the ordering among siblings are inherited from
 * {@link TreeEntity}; a node without a parent is a root category.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Entity
@Table(name = "customer_category",
		uniqueConstraints = @UniqueConstraint(name = "uk_customer_category_code", columnNames = "code"))
public class CustomerCategory extends TreeEntity<CustomerCategory> {

	@Column(name = "code", length = 64, nullable = false)
	private String code;

	@Column(name = "name", length = 100, nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private CustomerCategoryStatus status = CustomerCategoryStatus.ENABLED;

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

}

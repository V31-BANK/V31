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

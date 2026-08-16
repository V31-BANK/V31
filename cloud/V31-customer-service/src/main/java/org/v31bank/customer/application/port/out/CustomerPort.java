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

package org.v31bank.customer.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.customer.domain.constant.CustomerStatus;
import org.v31bank.customer.domain.model.Customer;
import org.v31bank.data.jpa.domain.PageQuery;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Output port for {@link Customer} persistence, implemented by the infrastructure layer.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface CustomerPort {

	Customer save(Customer customer);

	Optional<Customer> findById(UUID id);

	/**
	 * Find a page of customers, optionally filtered.
	 * @param email email fragment to match, or {@code null} for no filter
	 * @param status status to match, or {@code null} for no filter
	 * @param pageQuery the pagination request
	 * @return the page of matching customers, newest first
	 */
	PageResult<Customer> findPage(String email, CustomerStatus status, PageQuery pageQuery);

	void delete(Customer customer);

}

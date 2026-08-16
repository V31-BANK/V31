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

package org.v31bank.cbs.application.port.in;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.v31bank.cbs.application.dto.BankProductPageQuery;
import org.v31bank.cbs.domain.constant.BankProductCategory;
import org.v31bank.cbs.domain.constant.BankProductStatus;
import org.v31bank.cbs.domain.model.BankProduct;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.PageResponse;

/**
 * Use cases for managing the bank product catalogue.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface BankProductUseCase {

	/**
	 * Add a product to the catalogue, provided its code is not already taken.
	 * @param code the code the product is known by, unique
	 * @param name the display name
	 * @param category what kind of account it opens
	 * @param interestRate the annual rate as a fraction, or {@code null}
	 * @return the outcome of the command
	 */
	ApiResponse<BankProduct> create(String code, String name, BankProductCategory category, BigDecimal interestRate);

	Optional<BankProduct> get(UUID id);

	/**
	 * Find a page of products matching the filters carried by the query.
	 * @param query the filters and the pagination request
	 * @return the page of matching products
	 */
	PageResponse<BankProduct> page(BankProductPageQuery query);

	/**
	 * Update the product with the given identifier.
	 * @param id the product to update
	 * @param code the code, which must not belong to another product
	 * @param name the display name
	 * @param category what kind of account it opens
	 * @param status the new status, or {@code null} to leave it unchanged
	 * @param interestRate the annual rate as a fraction, or {@code null}
	 * @return the outcome of the command
	 */
	ApiResponse<BankProduct> update(UUID id, String code, String name, BankProductCategory category,
			BankProductStatus status, BigDecimal interestRate);

	/**
	 * Delete the product with the given identifier.
	 * @param id the product to delete
	 * @return the outcome of the command
	 */
	ApiResponse<BankProduct> delete(UUID id);

}

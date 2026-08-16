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

package org.v31bank.cbs.presentation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.v31bank.cbs.domain.constant.BankProductCategory;
import org.v31bank.cbs.domain.constant.BankProductStatus;
import org.v31bank.cbs.domain.model.BankProduct;

/**
 * API representation of a bank product.
 *
 * @param id the identifier it was issued when it was created
 * @param code the code it is known by, unique within the service
 * @param name what to call it
 * @param category which kind it is
 * @param status where it is in its lifecycle
 * @param interestRate the annual rate, as a fraction rather than a percentage
 * @param createdDate when it was created
 * @param lastModifiedDate when it was last changed
 * @author Xander Wang
 * @since 0.2.0
 */
public record BankProductResponse(UUID id, String code, String name, BankProductCategory category,
		BankProductStatus status, BigDecimal interestRate, Instant createdDate, Instant lastModifiedDate) {

	public static BankProductResponse from(BankProduct product) {
		return new BankProductResponse(product.getId(), product.getCode(), product.getName(), product.getCategory(),
				product.getStatus(), product.getInterestRate(), product.getCreatedDate(),
				product.getLastModifiedDate());
	}

}

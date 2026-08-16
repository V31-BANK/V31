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

package org.v31bank.cbs.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.v31bank.cbs.domain.constant.BankProductCategory;
import org.v31bank.cbs.domain.constant.BankProductStatus;

/**
 * A product the bank offers accounts against — a savings account, a term deposit, a
 * credit line.
 * <p>
 * Reference data: written rarely, read on nearly every account operation, and small
 * enough to hold entirely in memory. That is what makes Valkey a reasonable store for it
 * rather than only a cache in front of one.
 * <p>
 * The rate is a {@link BigDecimal} and the timestamps are {@link Instant}, which is what
 * the value serializer has to carry across untouched — a rate that comes back as a double
 * is a rate that is wrong.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class BankProduct {

	private UUID id;

	/**
	 * Code the product is known by across the bank, unique and quoted on statements.
	 */
	private String code;

	private String name;

	private BankProductCategory category;

	private BankProductStatus status = BankProductStatus.DRAFT;

	/**
	 * Annual rate as a fraction, so 2.5% is {@code 0.025}.
	 */
	private BigDecimal interestRate;

	private Instant createdDate;

	private Instant lastModifiedDate;

	public UUID getId() {
		return this.id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

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

	public BankProductCategory getCategory() {
		return this.category;
	}

	public void setCategory(BankProductCategory category) {
		this.category = category;
	}

	public BankProductStatus getStatus() {
		return this.status;
	}

	public void setStatus(BankProductStatus status) {
		this.status = status;
	}

	public BigDecimal getInterestRate() {
		return this.interestRate;
	}

	public void setInterestRate(BigDecimal interestRate) {
		this.interestRate = interestRate;
	}

	public Instant getCreatedDate() {
		return this.createdDate;
	}

	public void setCreatedDate(Instant createdDate) {
		this.createdDate = createdDate;
	}

	public Instant getLastModifiedDate() {
		return this.lastModifiedDate;
	}

	public void setLastModifiedDate(Instant lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

}

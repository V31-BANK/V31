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

package org.v31bank.transfer.domain.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.v31bank.data.jpa.domain.BaseEntity;
import org.v31bank.transfer.domain.constant.TransferLimitStatus;

/**
 * A ceiling on how much may move in a day under one policy.
 * <p>
 * Limits are the cheapest control a bank has: they cap what any single mistake or stolen
 * credential can cost before anybody notices.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Entity
@Table(name = "transfer_limit",
		uniqueConstraints = @UniqueConstraint(name = "uk_transfer_limit_code", columnNames = "code"))
public class TransferLimit extends BaseEntity {

	/**
	 * The code the policy is known by, unique.
	 */
	@Column(name = "code", length = 32, nullable = false)
	private String code;

	/**
	 * The display name.
	 */
	@Column(name = "name", length = 100, nullable = false)
	private String name;

	/**
	 * The most that may move in a rolling day, in the policy's asset.
	 */
	@Column(name = "daily_max", precision = 38, scale = 18)
	private BigDecimal dailyMax;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private TransferLimitStatus status = TransferLimitStatus.ACTIVE;

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

	public BigDecimal getDailyMax() {
		return this.dailyMax;
	}

	public void setDailyMax(BigDecimal dailyMax) {
		this.dailyMax = dailyMax;
	}

	public TransferLimitStatus getStatus() {
		return this.status;
	}

	public void setStatus(TransferLimitStatus status) {
		this.status = status;
	}

}

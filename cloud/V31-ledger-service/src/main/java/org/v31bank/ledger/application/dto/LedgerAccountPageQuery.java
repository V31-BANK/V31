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

package org.v31bank.ledger.application.dto;

import org.v31bank.data.jpa.domain.PageQuery;
import org.v31bank.ledger.domain.constant.LedgerAccountStatus;
import org.v31bank.ledger.domain.constant.LedgerAccountType;

/**
 * Paginated ledger account query with optional filters.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class LedgerAccountPageQuery extends PageQuery {

	/**
	 * Code fragment to match, case-insensitive.
	 */
	private String code;

	/**
	 * Type to match.
	 */
	private LedgerAccountType type;

	/**
	 * Status to match.
	 */
	private LedgerAccountStatus status;

	public String getCode() {
		return this.code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public LedgerAccountType getType() {
		return this.type;
	}

	public void setType(LedgerAccountType type) {
		this.type = type;
	}

	public LedgerAccountStatus getStatus() {
		return this.status;
	}

	public void setStatus(LedgerAccountStatus status) {
		this.status = status;
	}

}

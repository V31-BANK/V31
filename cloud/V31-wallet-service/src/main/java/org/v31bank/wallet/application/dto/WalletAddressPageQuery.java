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

package org.v31bank.wallet.application.dto;

import org.v31bank.data.jpa.domain.PageQuery;
import org.v31bank.wallet.domain.constant.WalletAddressStatus;

/**
 * Paginated wallet address query with optional filters.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class WalletAddressPageQuery extends PageQuery {

	/**
	 * Address fragment to match, case-insensitive.
	 */
	private String address;

	/**
	 * Asset to match.
	 */
	private String asset;

	/**
	 * Status to match.
	 */
	private WalletAddressStatus status;

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAsset() {
		return this.asset;
	}

	public void setAsset(String asset) {
		this.asset = asset;
	}

	public WalletAddressStatus getStatus() {
		return this.status;
	}

	public void setStatus(WalletAddressStatus status) {
		this.status = status;
	}

}

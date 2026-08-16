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

package org.v31bank.wallet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.v31bank.data.jpa.domain.BaseEntity;
import org.v31bank.wallet.domain.constant.WalletAddressStatus;

/**
 * A blockchain address a customer has registered to withdraw to.
 * <p>
 * Registering an address is the moment an operator can still stop a mistake: once a
 * withdrawal is broadcast to it, nothing recalls it. That is why an address arrives
 * {@code PENDING} and has to be approved before it can be used.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Entity
@Table(name = "wallet_address",
		uniqueConstraints = @UniqueConstraint(name = "uk_wallet_address_address", columnNames = "address"))
public class WalletAddress extends BaseEntity {

	/**
	 * The address itself, unique across the bank.
	 */
	@Column(name = "address", length = 128, nullable = false)
	private String address;

	/**
	 * What the customer calls it.
	 */
	@Column(name = "label", length = 100, nullable = false)
	private String label;

	/**
	 * The asset it can receive, for example {@code BTC}.
	 */
	@Column(name = "asset", length = 16, nullable = false)
	private String asset;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private WalletAddressStatus status = WalletAddressStatus.PENDING;

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getLabel() {
		return this.label;
	}

	public void setLabel(String label) {
		this.label = label;
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

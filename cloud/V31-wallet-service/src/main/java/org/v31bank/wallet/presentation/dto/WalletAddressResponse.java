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

package org.v31bank.wallet.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import org.v31bank.wallet.domain.constant.WalletAddressStatus;
import org.v31bank.wallet.domain.model.WalletAddress;

/**
 * API representation of a wallet address.
 *
 * @param id the identifier it was issued when it was created
 * @param address the address itself
 * @param label what to call it
 * @param asset which asset the address holds
 * @param status where it is in its lifecycle
 * @param createdDate when it was created
 * @param lastModifiedDate when it was last changed
 * @author Xander Wang
 * @since 0.2.0
 */
public record WalletAddressResponse(UUID id, String address, String label, String asset, WalletAddressStatus status,
		Instant createdDate, Instant lastModifiedDate) {

	public static WalletAddressResponse from(WalletAddress walletAddress) {
		return new WalletAddressResponse(walletAddress.getId(), walletAddress.getAddress(), walletAddress.getLabel(),
				walletAddress.getAsset(), walletAddress.getStatus(), walletAddress.getCreatedDate(),
				walletAddress.getLastModifiedDate());
	}

}

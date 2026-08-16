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

package org.v31bank.wallet.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.data.jpa.domain.PageResult;
import org.v31bank.wallet.application.dto.WalletAddressPageQuery;
import org.v31bank.wallet.application.port.in.WalletAddressUseCase;
import org.v31bank.wallet.application.port.out.WalletAddressPort;
import org.v31bank.wallet.domain.constant.WalletAddressStatus;
import org.v31bank.wallet.domain.model.WalletAddress;

/**
 * Default {@link WalletAddressUseCase} implementation.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Service
@Transactional
public class WalletAddressService implements WalletAddressUseCase {

	private final WalletAddressPort walletAddressRepository;

	public WalletAddressService(WalletAddressPort walletAddressRepository) {
		this.walletAddressRepository = walletAddressRepository;
	}

	@Override
	public ApiResponse<WalletAddress> create(String address, String label, String asset) {
		if (this.walletAddressRepository.existsByAddress(address)) {
			return ApiResponse.error(CommonErrorCode.CONFLICT, "Address '" + address + "' is already in use");
		}
		WalletAddress walletAddress = new WalletAddress();
		walletAddress.setAddress(address);
		walletAddress.setLabel(label);
		walletAddress.setAsset(asset);
		return ApiResponse.ok(this.walletAddressRepository.save(walletAddress));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<WalletAddress> get(UUID id) {
		return this.walletAddressRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<WalletAddress> page(WalletAddressPageQuery query) {
		return this.walletAddressRepository.findPage(query);
	}

	@Override
	public ApiResponse<WalletAddress> update(UUID id, String address, String label, String asset,
			WalletAddressStatus status) {
		Optional<WalletAddress> found = this.walletAddressRepository.findById(id);
		if (found.isEmpty()) {
			return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No wallet address exists with id " + id);
		}
		WalletAddress walletAddress = found.get();
		if (!walletAddress.getAddress().equals(address) && this.walletAddressRepository.existsByAddress(address)) {
			return ApiResponse.error(CommonErrorCode.CONFLICT, "Address '" + address + "' is already in use");
		}
		walletAddress.setAddress(address);
		walletAddress.setLabel(label);
		walletAddress.setAsset(asset);
		if (status != null) {
			walletAddress.setStatus(status);
		}
		return ApiResponse.ok(this.walletAddressRepository.save(walletAddress));
	}

	@Override
	public ApiResponse<WalletAddress> delete(UUID id) {
		Optional<WalletAddress> found = this.walletAddressRepository.findById(id);
		if (found.isEmpty()) {
			return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No wallet address exists with id " + id);
		}
		WalletAddress walletAddress = found.get();
		this.walletAddressRepository.delete(walletAddress);
		return ApiResponse.ok(walletAddress);
	}

}

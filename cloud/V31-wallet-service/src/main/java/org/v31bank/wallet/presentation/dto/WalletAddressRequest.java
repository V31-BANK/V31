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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.v31bank.wallet.domain.constant.WalletAddressStatus;

/**
 * Request body for creating or updating a wallet address.
 *
 * @param address the address, unique
 * @param label the display name
 * @param asset the asset
 * @param status the status; ignored on create, where a record always starts
 * {@code PENDING}, and left unchanged on update when {@code null}
 * @author Xander Wang
 * @since 0.2.0
 */
public record WalletAddressRequest(@NotBlank @Size(max = 128) String address, @NotBlank @Size(max = 100) String label,
		@NotBlank @Size(max = 16) String asset, WalletAddressStatus status) {

}

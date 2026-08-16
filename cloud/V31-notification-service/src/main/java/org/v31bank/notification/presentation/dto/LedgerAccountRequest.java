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

package org.v31bank.notification.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a ledger account through this service.
 *
 * @param code the code the account is known by, unique in the ledger
 * @param name the display name
 * @param type which side of the balance sheet it belongs to, for example {@code ASSET}
 * @param status the status; ignored on create, and left unchanged on update when absent
 * @author Xander Wang
 * @since 0.2.0
 */
public record LedgerAccountRequest(@NotBlank @Size(max = 32) String code, @NotBlank @Size(max = 100) String name,
		@NotBlank String type, String status) {

}

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

package org.v31bank.transfer.presentation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.v31bank.transfer.domain.constant.TransferLimitStatus;

/**
 * Request body for creating or updating a transfer limit.
 *
 * @param code the code, unique
 * @param name the display name
 * @param dailyMax the most that may be sent in a day, in the limit's own asset
 * @param status the status; ignored on create, where a record always starts
 * {@code ACTIVE}, and left unchanged on update when {@code null}
 * @author Xander Wang
 * @since 0.2.0
 */
public record TransferLimitRequest(@NotBlank @Size(max = 32) String code, @NotBlank @Size(max = 100) String name,
		@NotNull @PositiveOrZero @Digits(integer = 20, fraction = 18) BigDecimal dailyMax, TransferLimitStatus status) {

}

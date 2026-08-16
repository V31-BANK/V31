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

package org.v31bank.cbs.presentation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.v31bank.cbs.domain.constant.BankProductCategory;
import org.v31bank.cbs.domain.constant.BankProductStatus;

/**
 * Request body for adding or updating a bank product.
 *
 * @param code the code the product is known by, unique
 * @param name the display name
 * @param category what kind of account it opens
 * @param status the status; ignored on create, where a product always starts
 * {@code DRAFT}, and left unchanged on update when {@code null}
 * @param interestRate the annual rate as a fraction, so 2.5% is {@code 0.025}
 * @author Xander Wang
 * @since 0.2.0
 */
public record BankProductRequest(@NotBlank @Size(max = 32) String code, @NotBlank @Size(max = 100) String name,
		@NotNull BankProductCategory category, BankProductStatus status,
		@NotNull @PositiveOrZero @Digits(integer = 3, fraction = 8) BigDecimal interestRate) {

}

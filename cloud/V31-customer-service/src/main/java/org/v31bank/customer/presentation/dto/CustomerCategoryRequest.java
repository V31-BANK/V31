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

package org.v31bank.customer.presentation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.v31bank.customer.domain.constant.CustomerCategoryStatus;

/**
 * Request body for creating or updating a customer category.
 *
 * @param code the unique business code
 * @param name the display name
 * @param parentId the parent node, or {@code null} for a root category
 * @param sortOrder the position among siblings, or {@code null}
 * @param status the status; defaults to {@code ENABLED} on create and is left unchanged
 * on update when {@code null}
 * @author Xander Wang
 * @since 0.2.0
 */
public record CustomerCategoryRequest(@NotBlank @Size(max = 64) String code, @NotBlank @Size(max = 100) String name,
		UUID parentId, @PositiveOrZero Integer sortOrder, CustomerCategoryStatus status) {

}

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

package org.v31bank.compliance.presentation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.v31bank.compliance.domain.constant.ComplianceCaseStatus;
import org.v31bank.compliance.domain.constant.ComplianceCaseType;

/**
 * Request body for opening or updating a compliance case.
 *
 * @param caseNumber the reference the case is known by, unique
 * @param customerId the customer under investigation
 * @param type what is being investigated
 * @param status the status; ignored on create, where a case always starts {@code OPEN},
 * and left unchanged on update when {@code null}
 * @param summary why the case was opened
 * @author Xander Wang
 * @since 0.2.0
 */
public record ComplianceCaseRequest(@NotBlank @Size(max = 32) String caseNumber, @NotNull UUID customerId,
		@NotNull ComplianceCaseType type, ComplianceCaseStatus status, @Size(max = 500) String summary) {

}

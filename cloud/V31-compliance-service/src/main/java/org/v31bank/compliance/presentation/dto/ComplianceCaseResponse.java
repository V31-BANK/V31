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

import java.time.Instant;
import java.util.UUID;

import org.v31bank.compliance.domain.constant.ComplianceCaseStatus;
import org.v31bank.compliance.domain.constant.ComplianceCaseType;
import org.v31bank.compliance.domain.model.ComplianceCase;

/**
 * API representation of a compliance case.
 *
 * @param id the identifier it was issued when it was created
 * @param caseNumber the number the case is referred to by outside the system
 * @param customerId who the case is about
 * @param type which kind it is
 * @param status where it is in its lifecycle
 * @param summary what the case is about
 * @param createdDate when it was created
 * @param lastModifiedDate when it was last changed
 * @author Xander Wang
 * @since 0.2.0
 */
public record ComplianceCaseResponse(UUID id, String caseNumber, UUID customerId, ComplianceCaseType type,
		ComplianceCaseStatus status, String summary, Instant createdDate, Instant lastModifiedDate) {

	public static ComplianceCaseResponse from(ComplianceCase complianceCase) {
		return new ComplianceCaseResponse(complianceCase.getId(), complianceCase.getCaseNumber(),
				complianceCase.getCustomerId(), complianceCase.getType(), complianceCase.getStatus(),
				complianceCase.getSummary(), complianceCase.getCreatedDate(), complianceCase.getLastModifiedDate());
	}

}

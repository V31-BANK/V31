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

package org.v31bank.risk.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import org.v31bank.risk.domain.constant.RiskRuleStatus;
import org.v31bank.risk.domain.constant.RiskSeverity;
import org.v31bank.risk.domain.model.RiskRule;

/**
 * API representation of a risk rule.
 *
 * @param id the identifier it was issued when it was created
 * @param code the code it is known by, unique within the service
 * @param name what to call it
 * @param severity how much a match matters
 * @param status where it is in its lifecycle
 * @param createdDate when it was created
 * @param lastModifiedDate when it was last changed
 * @author Xander Wang
 * @since 0.2.0
 */
public record RiskRuleResponse(UUID id, String code, String name, RiskSeverity severity, RiskRuleStatus status,
		Instant createdDate, Instant lastModifiedDate) {

	public static RiskRuleResponse from(RiskRule riskRule) {
		return new RiskRuleResponse(riskRule.getId(), riskRule.getCode(), riskRule.getName(), riskRule.getSeverity(),
				riskRule.getStatus(), riskRule.getCreatedDate(), riskRule.getLastModifiedDate());
	}

}

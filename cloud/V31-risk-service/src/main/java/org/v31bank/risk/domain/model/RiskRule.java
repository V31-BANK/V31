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

package org.v31bank.risk.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.v31bank.data.jpa.domain.BaseEntity;
import org.v31bank.risk.domain.constant.RiskRuleStatus;
import org.v31bank.risk.domain.constant.RiskSeverity;

/**
 * A rule that decides whether an operation looks like something to stop.
 * <p>
 * Rules are held as data rather than code so that raising a threshold during an incident
 * does not need a deployment — and so that what was in force at the time a decision was
 * made can be reconstructed afterwards.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Entity
@Table(name = "risk_rule", uniqueConstraints = @UniqueConstraint(name = "uk_risk_rule_code", columnNames = "code"))
public class RiskRule extends BaseEntity {

	/**
	 * The code the rule is known by, unique.
	 */
	@Column(name = "code", length = 32, nullable = false)
	private String code;

	/**
	 * The display name.
	 */
	@Column(name = "name", length = 100, nullable = false)
	private String name;

	/**
	 * How hard a match is acted on.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "severity", length = 20, nullable = false)
	private RiskSeverity severity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private RiskRuleStatus status = RiskRuleStatus.DRAFT;

	public String getCode() {
		return this.code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public RiskSeverity getSeverity() {
		return this.severity;
	}

	public void setSeverity(RiskSeverity severity) {
		this.severity = severity;
	}

	public RiskRuleStatus getStatus() {
		return this.status;
	}

	public void setStatus(RiskRuleStatus status) {
		this.status = status;
	}

}

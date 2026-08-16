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

package org.v31bank.compliance.domain.model;

import java.time.Instant;
import java.util.UUID;

import org.v31bank.compliance.domain.constant.ComplianceCaseStatus;
import org.v31bank.compliance.domain.constant.ComplianceCaseType;

/**
 * An investigation opened against a customer — a failed identity check, a transfer that
 * matched a laundering pattern, a name that hit a sanctions list.
 * <p>
 * Unlike the JPA services, this model carries no persistence annotations and no mapped
 * superclass. jOOQ generates its own record per table, so the domain stays a plain object
 * and the adapter maps between the two. The audit fields are present but never set here:
 * they are filled in as the row is written, by the listener the jOOQ starter registers.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ComplianceCase {

	private UUID id;

	/**
	 * Reference the case is known by outside the system, unique and quoted in
	 * correspondence with regulators.
	 */
	private String caseNumber;

	private UUID customerId;

	private ComplianceCaseType type;

	private ComplianceCaseStatus status = ComplianceCaseStatus.OPEN;

	private String summary;

	private String createdBy;

	private Instant createdDate;

	private String lastModifiedBy;

	private Instant lastModifiedDate;

	public UUID getId() {
		return this.id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getCaseNumber() {
		return this.caseNumber;
	}

	public void setCaseNumber(String caseNumber) {
		this.caseNumber = caseNumber;
	}

	public UUID getCustomerId() {
		return this.customerId;
	}

	public void setCustomerId(UUID customerId) {
		this.customerId = customerId;
	}

	public ComplianceCaseType getType() {
		return this.type;
	}

	public void setType(ComplianceCaseType type) {
		this.type = type;
	}

	public ComplianceCaseStatus getStatus() {
		return this.status;
	}

	public void setStatus(ComplianceCaseStatus status) {
		this.status = status;
	}

	public String getSummary() {
		return this.summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getCreatedBy() {
		return this.createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Instant getCreatedDate() {
		return this.createdDate;
	}

	public void setCreatedDate(Instant createdDate) {
		this.createdDate = createdDate;
	}

	public String getLastModifiedBy() {
		return this.lastModifiedBy;
	}

	public void setLastModifiedBy(String lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

	public Instant getLastModifiedDate() {
		return this.lastModifiedDate;
	}

	public void setLastModifiedDate(Instant lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

}

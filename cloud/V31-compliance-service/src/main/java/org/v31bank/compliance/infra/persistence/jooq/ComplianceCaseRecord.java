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

package org.v31bank.compliance.infra.persistence.jooq;

import java.io.Serial;

import org.jooq.impl.UpdatableRecordImpl;

/**
 * A row of {@code compliance_case}.
 * <p>
 * Being an updatable record — rather than a plain one — is what makes {@code insert()},
 * {@code update()} and {@code store()} available on it, and those are the calls the audit
 * listener registered by the jOOQ starter observes. Writing the same row through
 * {@code dsl.insertInto(...)} would work and would not be audited.
 * <p>
 * It carries no typed accessors: the adapter reads and writes through the fields on
 * {@link ComplianceCaseTable}, which keeps this class to what jOOQ needs to instantiate
 * it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ComplianceCaseRecord extends UpdatableRecordImpl<ComplianceCaseRecord> {

	@Serial
	private static final long serialVersionUID = 1L;

	public ComplianceCaseRecord() {
		super(ComplianceCaseTable.COMPLIANCE_CASE);
	}

}

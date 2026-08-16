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

package org.v31bank.data.jpa.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FixedAuditorAware}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class FixedAuditorAwareTests {

	@Test
	void reportsTheAuditorItWasGiven() {
		assertThat(new FixedAuditorAware("system").getCurrentAuditor()).contains("system");
	}

	/**
	 * An empty result leaves the audit columns unset rather than stamping them with the
	 * word "null", which would read as a real user in an audit trail.
	 */
	@Test
	void reportsNobodyWhenThereIsNoAuditor() {
		assertThat(new FixedAuditorAware(null).getCurrentAuditor()).isEmpty();
	}

}

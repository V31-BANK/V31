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

package org.v31bank.jooq.audit;

import java.util.Optional;

/**
 * Answers who is acting, so that {@link AuditRecordListener} can record it against every
 * row a request writes.
 * <p>
 * This is jOOQ's counterpart to Spring Data's {@code AuditorAware}, declared here rather
 * than reused so that a service using jOOQ alone does not have to bring in Spring Data to
 * say who its user is. An application backed by both implements the two against the same
 * source.
 * <p>
 * The auditor is read once per statement, on the thread running it. An implementation
 * reading from a security context or a request scope has to account for work handed to
 * another thread, where that context is not automatically present.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@FunctionalInterface
public interface AuditorSupplier {

	/**
	 * Return who is currently acting.
	 * @return the auditor, or empty when nobody is identified — a scheduled job, a
	 * migration, a request that has not been authenticated. The audit columns are then
	 * left as they are rather than being filled with a placeholder.
	 */
	Optional<String> currentAuditor();

}

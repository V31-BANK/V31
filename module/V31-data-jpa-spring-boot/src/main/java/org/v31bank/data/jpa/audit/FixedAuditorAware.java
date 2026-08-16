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

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.AuditorAware;

/**
 * {@link AuditorAware} implementation that always returns a fixed auditor. Used as a
 * fallback when the application does not provide its own {@link AuditorAware} bean,
 * typically one backed by the current authentication.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@NullMarked
public class FixedAuditorAware implements AuditorAware<String> {

	private final @Nullable String auditor;

	public FixedAuditorAware(@Nullable String auditor) {
		this.auditor = auditor;
	}

	@Override
	public Optional<String> getCurrentAuditor() {
		return Optional.ofNullable(this.auditor);
	}

}

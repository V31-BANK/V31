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

package org.v31bank.data.jpa.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for V31 Data JPA.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@ConfigurationProperties("v31.data.jpa")
public class V31DataJpaProperties {

	private final Auditing auditing = new Auditing();

	public Auditing getAuditing() {
		return this.auditing;
	}

	/**
	 * Auditing properties.
	 */
	public static class Auditing {

		/**
		 * Whether to enable JPA auditing.
		 */
		private boolean enabled = true;

		/**
		 * Auditor recorded when no application-provided AuditorAware bean exists.
		 */
		private String defaultAuditor = "system";

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getDefaultAuditor() {
			return this.defaultAuditor;
		}

		public void setDefaultAuditor(String defaultAuditor) {
			this.defaultAuditor = defaultAuditor;
		}

	}

}

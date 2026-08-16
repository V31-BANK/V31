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

package org.v31bank.web.autoconfigure;

import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import org.v31bank.web.filter.RequestIdFilter;

/**
 * Puts the request identifier into every log line.
 * <p>
 * Carrying an identifier through the platform achieves nothing while no log line prints
 * it. Spring Boot's default console pattern already reserves a place for exactly this —
 * {@code logging.pattern.correlation} — so filling it in is all that is needed, and a
 * service that sets its own pattern keeps it.
 * <p>
 * Contributed from here rather than repeated in eight {@code application.yml} files: the
 * module that establishes the identifier is the one that knows what it is called.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class V31LoggingCorrelationEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String PROPERTY_NAME = "logging.pattern.correlation";

	private static final String PROPERTY_SOURCE_NAME = "v31-logging-correlation";

	/**
	 * Trailing space included so the identifier does not run into the message. A line
	 * written outside any request — startup, a scheduled job — prints empty brackets,
	 * which is what Spring Boot's own correlation pattern does and is the reason this
	 * place in the line exists at all.
	 */
	private static final String CORRELATION_PATTERN = "[%X{" + RequestIdFilter.MDC_KEY + ":-}] ";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (environment.containsProperty(PROPERTY_NAME)) {
			return;
		}
		environment.getPropertySources()
			.addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of(PROPERTY_NAME, CORRELATION_PATTERN)));
	}

}

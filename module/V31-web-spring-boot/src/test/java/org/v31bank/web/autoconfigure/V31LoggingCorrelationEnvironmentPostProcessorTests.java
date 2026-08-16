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

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.support.EnvironmentPostProcessorsFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link V31LoggingCorrelationEnvironmentPostProcessor}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class V31LoggingCorrelationEnvironmentPostProcessorTests {

	private static final String PROPERTY_NAME = "logging.pattern.correlation";

	private final ConfigurableEnvironment environment = new StandardEnvironment();

	private final SpringApplication application = new SpringApplication();

	private final V31LoggingCorrelationEnvironmentPostProcessor postProcessor = new V31LoggingCorrelationEnvironmentPostProcessor();

	@Test
	void postProcessEnvironmentAddsCorrelationPattern() {
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty(PROPERTY_NAME)).isEqualTo("[%X{requestId:-}] ");
	}

	@Test
	void postProcessEnvironmentWhenPatternAlreadySetBacksOff() {
		TestPropertyValues.of(PROPERTY_NAME + "=custom").applyTo(this.environment);
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty(PROPERTY_NAME)).isEqualTo("custom");
	}

	/**
	 * Guards the {@code spring.factories} key rather than the class: a post-processor
	 * that Spring Boot never loads is indistinguishable from one that was deleted, and
	 * the key is the one part of the contract the compiler cannot check.
	 */
	@Test
	void isRegisteredInSpringFactories() {
		DeferredLogFactory logFactory = Supplier::get;
		List<EnvironmentPostProcessor> postProcessors = EnvironmentPostProcessorsFactory
			.fromSpringFactories(getClass().getClassLoader())
			.getEnvironmentPostProcessors(logFactory, new DefaultBootstrapContext());
		assertThat(postProcessors).hasAtLeastOneElementOfType(V31LoggingCorrelationEnvironmentPostProcessor.class);
	}

	@Test
	void runsWhenApplicationStarts() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
			.web(WebApplicationType.NONE)
			.run()) {
			assertThat(context.getEnvironment().getProperty(PROPERTY_NAME)).isEqualTo("[%X{requestId:-}] ");
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

	}

}

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

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.v31bank.web.advice.ApiResponseExceptionHandler;
import org.v31bank.web.advice.DataAccessExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link V31WebAutoConfiguration}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class V31WebAutoConfigurationTests {

	private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(V31WebAutoConfiguration.class));

	@Test
	void registersBothHandlers() {
		this.runner.run((context) -> {
			assertThat(context).hasSingleBean(ApiResponseExceptionHandler.class);
			assertThat(context).hasSingleBean(DataAccessExceptionHandler.class);
		});
	}

	@Test
	void addsNothingWhenTurnedOff() {
		this.runner.withPropertyValues("v31.web.exception-handling.enabled=false").run((context) -> {
			assertThat(context).doesNotHaveBean(ApiResponseExceptionHandler.class);
			assertThat(context).doesNotHaveBean(DataAccessExceptionHandler.class);
		});
	}

	/**
	 * A service with a reason to answer differently keeps that reason by declaring its
	 * own bean, rather than by turning the module off and losing the rest of it.
	 */
	@Test
	void backsOffFromAnApplicationSuppliedHandler() {
		this.runner.withUserConfiguration(CustomHandlerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ApiResponseExceptionHandler.class);
			assertThat(context.getBean(ApiResponseExceptionHandler.class)).isInstanceOf(CustomHandler.class);
		});
	}

	/**
	 * A batch job or a message consumer has no controllers for an advice to advise.
	 */
	@Test
	void addsNothingOutsideAWebApplication() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(V31WebAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(ApiResponseExceptionHandler.class));
	}

	@Test
	void bindsItsProperties() {
		this.runner
			.run((context) -> assertThat(context.getBean(V31WebProperties.class).getExceptionHandling().isEnabled())
				.isTrue());
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHandlerConfiguration {

		@Bean
		ApiResponseExceptionHandler apiResponseExceptionHandler() {
			return new CustomHandler();
		}

	}

	static class CustomHandler extends ApiResponseExceptionHandler {

	}

}

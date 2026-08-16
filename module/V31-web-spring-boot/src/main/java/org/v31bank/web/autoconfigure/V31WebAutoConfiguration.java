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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.web.servlet.DispatcherServlet;

import org.v31bank.web.advice.ApiResponseExceptionHandler;
import org.v31bank.web.advice.DataAccessExceptionHandler;
import org.v31bank.web.filter.RequestIdFilter;

/**
 * {@link AutoConfiguration Auto-configuration} for V31 web support: makes every failure a
 * service reports arrive in the same envelope as everything it succeeds with.
 * <p>
 * Both handlers back off from one the application declared itself, so a service with a
 * reason to answer differently keeps that reason by declaring its own bean rather than by
 * turning the module off.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnBooleanProperty(name = "v31.web.exception-handling.enabled", matchIfMissing = true)
@EnableConfigurationProperties(V31WebProperties.class)
public class V31WebAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ApiResponseExceptionHandler apiResponseExceptionHandler() {
		return new ApiResponseExceptionHandler();
	}

	/**
	 * Gives every request something to be traced by. Stands aside where a service already
	 * establishes one for its outbound calls.
	 * @return the filter
	 */
	@Bean
	@ConditionalOnMissingBean
	public RequestIdFilter requestIdFilter() {
		return new RequestIdFilter();
	}

	/**
	 * Registered only where a data access failure is something that can actually happen.
	 * <p>
	 * Nested and guarded at class level rather than on the {@code @Bean} method, because
	 * the handler names {@code DataIntegrityViolationException} in a method signature:
	 * Spring reflects over an advice's methods to find its handlers, so on a service
	 * without {@code spring-tx} the class must never be reached at all.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(DataAccessException.class)
	public static class DataAccessExceptionHandlerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public DataAccessExceptionHandler dataAccessExceptionHandler() {
			return new DataAccessExceptionHandler();
		}

	}

}

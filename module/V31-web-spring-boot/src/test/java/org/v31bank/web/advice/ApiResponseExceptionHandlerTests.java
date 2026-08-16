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

package org.v31bank.web.advice;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.v31bank.core.constant.ApiHeaders;
import org.v31bank.core.exception.BusinessException;
import org.v31bank.core.response.CommonErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link ApiResponseExceptionHandler}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class ApiResponseExceptionHandlerTests {

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TestController())
		.setControllerAdvice(new ApiResponseExceptionHandler())
		.setValidator(validator())
		.build();

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void reportsARefusalWithTheCodeAndStatusItCarried() throws Exception {
		this.mvc.perform(get("/refuse/NOT_FOUND"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("No wallet exists with id 7"));
	}

	@Test
	void placesARefusalByTheCodesOwnStatus() throws Exception {
		this.mvc.perform(get("/refuse/RATE_LIMITED")).andExpect(status().isTooManyRequests());
		this.mvc.perform(get("/refuse/DEPENDENCY_TIMEOUT")).andExpect(status().isGatewayTimeout());
	}

	@Test
	void namesEveryRejectedField() throws Exception {
		this.mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.code()))
			.andExpect(jsonPath("$.violations.length()").value(2))
			.andExpect(jsonPath("$.violations[?(@.field=='email')]").exists())
			.andExpect(jsonPath("$.violations[?(@.field=='fullName')]").exists());
	}

	/**
	 * The value is what makes a validation failure dangerous to echo: these endpoints are
	 * sent names, e-mail addresses and account identifiers, and the response is the one
	 * part of a failure a caller is most likely to log.
	 */
	@Test
	void keepsTheRejectedValueOutOfTheResponse() throws Exception {
		this.mvc
			.perform(post("/customers").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ada@v31bank.org\",\"fullName\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$..*[?(@ == 'ada@v31bank.org')]").doesNotExist());
	}

	/**
	 * The log file is the other place a rejected value ends up. Spring's own message for
	 * a validation failure quotes every one of them, and a rejected request is the most
	 * common kind there is — so logging that message would put customer data in the log
	 * of every service, at volume.
	 */
	@Test
	void keepsTheRejectedValueOutOfTheLogToo() throws Exception {
		ListAppender<ILoggingEvent> appender = attachAppender();
		try {
			this.mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ada@v31bank.org\",\"fullName\":\"\"}"));
			assertThat(appender.list).isNotEmpty()
				.noneMatch((event) -> event.getFormattedMessage().contains("ada@v31bank.org"));
		}
		finally {
			detachAppender(appender);
		}
	}

	@Test
	void reportsAMalformedEmailRatherThanStoringIt() throws Exception {
		this.mvc
			.perform(post("/customers").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"not-an-email\",\"fullName\":\"Ada\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.violations[0].field").value("email"));
	}

	/**
	 * The message names a table, a constraint or a host often enough that it cannot be
	 * sent to whoever made the call.
	 */
	@Test
	void saysNothingAboutAnUnrecognisedFailure() throws Exception {
		this.mvc.perform(get("/explode"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value(CommonErrorCode.INTERNAL_ERROR.code()))
			.andExpect(jsonPath("$.message").value(CommonErrorCode.INTERNAL_ERROR.defaultMessage()))
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers
				.not(org.hamcrest.Matchers.containsString("jdbc:postgresql://ledger-primary.internal:5432"))));
	}

	@Test
	void answersAnUnreadableBodyAsTheCallersFault() throws Exception {
		this.mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content("not json at all"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.code()));
	}

	@Test
	void answersAPathVariableThatWillNotParseAsTheCallersFault() throws Exception {
		this.mvc.perform(get("/customers/not-a-number")).andExpect(status().isBadRequest());
	}

	@Test
	void stampsTheIdentifierTheRequestArrivedWith() throws Exception {
		this.mvc.perform(get("/refuse/CONFLICT").header(ApiHeaders.REQUEST_ID, "REQ-9142"))
			.andExpect(jsonPath("$.traceId").value("REQ-9142"));
	}

	@Test
	void stampsAValidationFailureToo() throws Exception {
		this.mvc
			.perform(post("/customers").contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.header(ApiHeaders.REQUEST_ID, "REQ-7"))
			.andExpect(jsonPath("$.traceId").value("REQ-7"));
	}

	@Test
	void fallsBackToTheIdentifierAlreadyBeingLoggedUnder() throws Exception {
		MDC.put("requestId", "FROM-MDC");
		this.mvc.perform(get("/explode")).andExpect(jsonPath("$.traceId").value("FROM-MDC"));
	}

	@Test
	void leavesTheIdentifierOutWhenThereIsNoneToReport() throws Exception {
		this.mvc.perform(get("/refuse/CONFLICT")).andExpect(jsonPath("$.traceId").isEmpty());
	}

	@Test
	void leavesASucceedingRequestAlone() throws Exception {
		this.mvc.perform(get("/ok")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"));
	}

	private static ListAppender<ILoggingEvent> attachAppender() {
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(ApiResponseExceptionHandler.class);
		logger.setLevel(Level.DEBUG);
		logger.addAppender(appender);
		return appender;
	}

	private static void detachAppender(ListAppender<ILoggingEvent> appender) {
		((Logger) org.slf4j.LoggerFactory.getLogger(ApiResponseExceptionHandler.class)).detachAppender(appender);
	}

	private static LocalValidatorFactoryBean validator() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		return validator;
	}

	/**
	 * Stands in for a service's controller, raising what one raises.
	 * <p>
	 * The path variables are named explicitly rather than left to be read off the
	 * bytecode. A module is compiled without the Spring Boot plugin, which is what adds
	 * {@code -parameters} to the services, so a name omitted here would resolve or not
	 * depending on a compiler flag this module does not control.
	 */
	@RestController
	static class TestController {

		@GetMapping("/refuse/{code}")
		String refuse(@PathVariable("code") String code) {
			CommonErrorCode errorCode = CommonErrorCode.valueOf(code);
			throw new BusinessException(errorCode, (errorCode == CommonErrorCode.NOT_FOUND)
					? "No wallet exists with id 7" : errorCode.defaultMessage());
		}

		@GetMapping("/explode")
		String explode() {
			throw new IllegalStateException(
					"could not connect to jdbc:postgresql://ledger-primary.internal:5432/ledger");
		}

		@GetMapping("/ok")
		org.v31bank.core.response.ApiResponse<String> ok() {
			return org.v31bank.core.response.ApiResponse.ok("fine");
		}

		@GetMapping("/customers/{id}")
		String byId(@PathVariable("id") long id) {
			return String.valueOf(id);
		}

		@PostMapping("/customers")
		String create(@Valid @RequestBody CustomerBody body) {
			return body.email();
		}

	}

	record CustomerBody(@NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(max = 100) String fullName) {

	}

}

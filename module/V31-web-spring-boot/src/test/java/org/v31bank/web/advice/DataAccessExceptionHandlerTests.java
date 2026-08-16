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

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.v31bank.core.response.CommonErrorCode;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link DataAccessExceptionHandler}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class DataAccessExceptionHandlerTests {

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TestController())
		.setControllerAdvice(new DataAccessExceptionHandler(), new ApiResponseExceptionHandler())
		.build();

	/**
	 * Two requests creating the same record both pass the uniqueness check and one loses
	 * at the index. Reported as a {@code 500} it tells the caller to retry something that
	 * will never succeed.
	 */
	@Test
	void reportsAWriteTheDatabaseRefusedAsAConflict() throws Exception {
		this.mvc.perform(get("/duplicate"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(CommonErrorCode.CONFLICT.code()));
	}

	@Test
	void saysNothingAboutTheConstraintThatRefusedIt() throws Exception {
		this.mvc.perform(get("/duplicate"))
			.andExpect(jsonPath("$.message").value(not(containsString("uk_customer_email"))))
			.andExpect(jsonPath("$.message").value(not(containsString("customer_service.customer"))));
	}

	@Test
	void reportsALostRaceForTheSameRowAsAConflictToo() throws Exception {
		this.mvc.perform(get("/stale"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value(CommonErrorCode.CONFLICT.code()))
			.andExpect(jsonPath("$.message").value("The record changed while this request was in flight"));
	}

	/**
	 * Its precedence has to hold, or the catch-all in the other advice claims these first
	 * and answers {@code 500}.
	 */
	@Test
	void takesPrecedenceOverTheCatchAll() throws Exception {
		this.mvc.perform(get("/duplicate")).andExpect(status().isConflict());
		this.mvc.perform(get("/other")).andExpect(status().isInternalServerError());
	}

	/**
	 * Raises what a repository raises.
	 */
	@RestController
	static class TestController {

		@GetMapping("/duplicate")
		String duplicate() {
			throw new DataIntegrityViolationException(
					"duplicate key value violates unique constraint \"uk_customer_email\" "
							+ "for table customer_service.customer");
		}

		@GetMapping("/stale")
		String stale() {
			throw new OptimisticLockingFailureException("Row was updated by another transaction");
		}

		@GetMapping("/other")
		String other() {
			throw new IllegalStateException("something else entirely");
		}

	}

}

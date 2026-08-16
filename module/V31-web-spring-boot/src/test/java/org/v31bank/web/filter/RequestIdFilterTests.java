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

package org.v31bank.web.filter;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.v31bank.core.constant.ApiHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RequestIdFilter}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class RequestIdFilterTests {

	private final RequestIdFilter filter = new RequestIdFilter();

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void keepsTheIdentifierTheCallerSent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(ApiHeaders.REQUEST_ID, "REQ-4417");
		MockHttpServletResponse response = new MockHttpServletResponse();

		String[] logged = new String[1];
		this.filter.doFilter(request, response, capture(logged));

		assertThat(logged[0]).isEqualTo("REQ-4417");
		assertThat(response.getHeader(ApiHeaders.REQUEST_ID)).isEqualTo("REQ-4417");
	}

	/**
	 * A caller that sent none is told, in the response, which identifier to quote.
	 */
	@Test
	void issuesOneWhenTheCallerSentNone() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		String[] logged = new String[1];
		this.filter.doFilter(new MockHttpServletRequest(), response, capture(logged));

		assertThat(logged[0]).isNotNull();
		assertThat(response.getHeader(ApiHeaders.REQUEST_ID)).isEqualTo(logged[0]);
		assertThat(UUID.fromString(logged[0]).version()).isEqualTo(7);
	}

	/**
	 * The value reaches a response header and every log line of the request. One
	 * containing a newline would let a caller forge log entries or terminate a header
	 * early.
	 */
	@Test
	void refusesToEchoAnIdentifierItWouldNotBeSafeTo() throws Exception {
		for (String unsafe : new String[] { "REQ\r\nInjected: yes", "has spaces", "x".repeat(65), "" }) {
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addHeader(ApiHeaders.REQUEST_ID, unsafe);
			MockHttpServletResponse response = new MockHttpServletResponse();

			String[] logged = new String[1];
			this.filter.doFilter(request, response, capture(logged));

			assertThat(logged[0]).isNotEqualTo(unsafe);
			assertThat(UUID.fromString(logged[0]).version()).isEqualTo(7);
		}
	}

	/**
	 * The thread goes back to a pool. An identifier left on it would be attributed to
	 * whatever it serves next.
	 */
	@Test
	void leavesNothingBehindOnTheThread() throws Exception {
		this.filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

		assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
	}

	/**
	 * Where a service also carries context onward over gRPC, that filter runs first and
	 * has already established the identifier. Deriving a second one here would split the
	 * request into two traces.
	 */
	@Test
	void standsAsideWhenOneIsAlreadyEstablished() throws Exception {
		MDC.put(RequestIdFilter.MDC_KEY, "ESTABLISHED-EARLIER");
		MockHttpServletResponse response = new MockHttpServletResponse();

		String[] logged = new String[1];
		this.filter.doFilter(new MockHttpServletRequest(), response, capture(logged));

		assertThat(logged[0]).isEqualTo("ESTABLISHED-EARLIER");
		assertThat(response.getHeader(ApiHeaders.REQUEST_ID)).isNull();
		assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isEqualTo("ESTABLISHED-EARLIER");
	}

	/**
	 * Captures what a handler running inside the filter would log under.
	 */
	private static MockFilterChain capture(String[] logged) {
		return new MockFilterChain() {
			@Override
			public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
				logged[0] = MDC.get(RequestIdFilter.MDC_KEY);
			}
		};
	}

}

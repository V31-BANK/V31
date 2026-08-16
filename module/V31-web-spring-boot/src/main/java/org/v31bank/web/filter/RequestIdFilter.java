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

import java.io.IOException;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import org.v31bank.core.constant.ApiHeaders;
import org.v31bank.core.util.Uuids;

/**
 * Gives every request an identifier that its log lines and its response both carry.
 * <p>
 * A customer reports that a transfer failed at about nine o'clock. Without an identifier
 * on the response there is nothing to search for, and without one in the log lines there
 * is nothing to find — so the investigation starts by guessing at timestamps. This is
 * what makes that a lookup instead.
 * <p>
 * The caller's identifier is kept when it is safe to echo, so a request that has already
 * been given one by a gateway stays one trace rather than becoming two. A caller that
 * sent none is told, in the response header, which identifier it was served under.
 *
 * <h2>Standing aside</h2>
 *
 * A service that also carries context onward over gRPC has a filter of its own that runs
 * first and establishes the same identifier. This one then does nothing: re-deriving it
 * would replace the identifier already being logged under with a second one, and the two
 * halves of the request would no longer join up. Checking what is in the {@link MDC}
 * rather than which beans exist keeps the two modules from having to know about each
 * other.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestIdFilter extends OncePerRequestFilter {

	/**
	 * Key the identifier is logged under, referenced from the logging pattern as
	 * {@code %X{requestId}}.
	 */
	public static final String MDC_KEY = "requestId";

	/**
	 * What an identifier arriving from outside has to look like to be echoed.
	 * <p>
	 * It ends up in a response header and in every log line of the request. An unchecked
	 * one would let a caller forge log entries by embedding newlines, or terminate a
	 * header early.
	 */
	private static final Pattern ACCEPTED = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (MDC.get(MDC_KEY) != null) {
			chain.doFilter(request, response);
			return;
		}
		String requestId = resolve(request.getHeader(ApiHeaders.REQUEST_ID));
		response.setHeader(ApiHeaders.REQUEST_ID, requestId);
		MDC.put(MDC_KEY, requestId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			// Removed rather than left: the thread goes back to a pool, and an
			// identifier left on it would be attributed to whatever it serves next.
			MDC.remove(MDC_KEY);
		}
	}

	/**
	 * Return the identifier to serve a request under, keeping the caller's only when it
	 * is safe to echo.
	 * @param supplied the value that arrived, which may be {@code null}
	 * @return the identifier for this request
	 */
	private static String resolve(String supplied) {
		if (supplied != null && ACCEPTED.matcher(supplied).matches()) {
			return supplied;
		}
		return Uuids.timeOrdered().toString();
	}

}

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

package org.v31bank.grpc.web;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import org.v31bank.core.constant.ApiHeaders;
import org.v31bank.grpc.context.GrpcRequestId;
import org.v31bank.grpc.context.RequestContext;

/**
 * Where a request's context enters the platform.
 * <p>
 * The gRPC interceptors carry values from one service to the next, but something has to
 * put them there in the first place, and for most requests that is an HTTP call from
 * outside. Without this filter the client interceptor finds nothing to send: the first
 * service in the chain logs one identifier, the second issues another, and the two halves
 * of the same request cannot be joined up.
 * <p>
 * Runs first, so that everything after it — including anything that logs — is already
 * inside the request's context.
 * <p>
 * Named for what it does rather than for what it holds, because Spring MVC already
 * registers a {@code requestContextFilter} of its own and two beans cannot share a name.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HeaderPropagationFilter extends OncePerRequestFilter {

	private final List<String> propagated;

	/**
	 * Create a filter.
	 * @param propagatedHeaders the header names to carry beyond the request identifier
	 */
	public HeaderPropagationFilter(Collection<String> propagatedHeaders) {
		this.propagated = List.copyOf(propagatedHeaders);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = GrpcRequestId.resolve(request.getHeader(ApiHeaders.REQUEST_ID));
		Map<String, String> values = RequestContext.newValues();
		values.put(GrpcRequestId.HEADER_NAME, requestId);
		for (String name : this.propagated) {
			RequestContext.put(values, name, request.getHeader(name));
		}
		// Echoed so that a client reporting a problem has something to quote, and
		// so that one which sent no identifier learns the one it was served under.
		response.setHeader(ApiHeaders.REQUEST_ID, requestId);
		MDC.put(GrpcRequestId.MDC_KEY, requestId);
		try (RequestContext.Scope scope = RequestContext.attach(values)) {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove(GrpcRequestId.MDC_KEY);
		}
	}

}

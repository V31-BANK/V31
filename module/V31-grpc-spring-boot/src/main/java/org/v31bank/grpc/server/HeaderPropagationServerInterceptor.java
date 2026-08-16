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

package org.v31bank.grpc.server;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.MDC;

import org.v31bank.grpc.context.GrpcRequestId;
import org.v31bank.grpc.context.RequestContext;

/**
 * Takes what a call arrived carrying and makes it available for the rest of it —
 * including for the calls made onward.
 * <p>
 * Every call gets a request identifier: the caller's when it sent one, a new one when it
 * did not. Anything else listed in {@code v31.grpc.propagation.headers} is carried
 * through as it arrived.
 * <p>
 * The values go into the gRPC context, which is what makes them reachable from the
 * handler and from the client interceptor when that handler calls another service. The
 * {@link MDC} is set and cleared around each callback rather than once around the whole
 * call, because the callbacks do not all run on the same thread and a value left behind
 * on a pooled thread would be attributed to whatever that thread served next.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class HeaderPropagationServerInterceptor implements ServerInterceptor {

	private final List<Metadata.Key<String>> propagated;

	/**
	 * Create an interceptor.
	 * @param propagatedHeaders the names to carry through beyond the request identifier,
	 * lower case as gRPC requires
	 */
	public HeaderPropagationServerInterceptor(Collection<String> propagatedHeaders) {
		this.propagated = propagatedHeaders.stream()
			.map((name) -> Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER))
			.toList();
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
			ServerCallHandler<ReqT, RespT> next) {
		String requestId = GrpcRequestId.resolve(headers.get(GrpcRequestId.METADATA_KEY));
		Map<String, String> values = RequestContext.newValues();
		values.put(GrpcRequestId.HEADER_NAME, requestId);
		for (Metadata.Key<String> key : this.propagated) {
			RequestContext.put(values, key.originalName(), headers.get(key));
		}
		Context context = Context.current()
			.withValue(RequestContext.CONTEXT_KEY, Map.copyOf(values))
			.withValue(GrpcRequestId.CONTEXT_KEY, requestId);
		return new MdcServerCallListener<>(Contexts.interceptCall(context, call, headers, next), requestId);
	}

	/**
	 * Puts the identifier in the {@link MDC} for the duration of each callback.
	 *
	 * @param <ReqT> the request type
	 */
	private static final class MdcServerCallListener<ReqT> extends SimpleForwardingServerCallListener<ReqT> {

		private final String requestId;

		private MdcServerCallListener(ServerCall.Listener<ReqT> delegate, String requestId) {
			super(delegate);
			this.requestId = requestId;
		}

		@Override
		public void onMessage(ReqT message) {
			run(() -> super.onMessage(message));
		}

		@Override
		public void onHalfClose() {
			run(super::onHalfClose);
		}

		@Override
		public void onCancel() {
			run(super::onCancel);
		}

		@Override
		public void onComplete() {
			run(super::onComplete);
		}

		@Override
		public void onReady() {
			run(super::onReady);
		}

		private void run(Runnable callback) {
			MDC.put(GrpcRequestId.MDC_KEY, this.requestId);
			try {
				callback.run();
			}
			finally {
				MDC.remove(GrpcRequestId.MDC_KEY);
			}
		}

	}

}

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

package org.v31bank.grpc.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import org.v31bank.grpc.context.RequestContext;

/**
 * Sends onward whatever the request being served arrived carrying, so that a trace, a
 * tenant or a locale does not stop at the edge of a service.
 * <p>
 * Without it, following one customer's complaint through the platform means finding the
 * identifier in one service's logs, working out from timestamps which call it made, and
 * starting again — for every hop.
 * <p>
 * Nothing is sent when there is nothing to send. A scheduled job calling another service
 * is not part of anybody's request, and inventing values here would only make it look
 * like it was.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class HeaderPropagationClientInterceptor implements ClientInterceptor {

	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
			CallOptions callOptions, Channel next) {
		return new SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
			@Override
			public void start(Listener<RespT> responseListener, Metadata headers) {
				RequestContext.current()
					.forEach((name, value) -> headers.put(Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER),
							value));
				super.start(responseListener, headers);
			}
		};
	}

}

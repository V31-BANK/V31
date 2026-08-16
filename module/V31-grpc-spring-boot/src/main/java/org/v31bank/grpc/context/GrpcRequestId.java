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

package org.v31bank.grpc.context;

import java.util.regex.Pattern;

import io.grpc.Context;
import io.grpc.Metadata;
import org.slf4j.MDC;

import org.v31bank.core.constant.ApiHeaders;
import org.v31bank.core.util.Uuids;

/**
 * The identifier correlating everything done while serving one request, and how it
 * travels.
 * <p>
 * The same identifier the HTTP layer carries in {@link ApiHeaders#REQUEST_ID}, under the
 * lower-case name gRPC requires of a metadata key — so a request that arrives over REST
 * and continues over gRPC is one trace rather than two.
 * <p>
 * It is one of the values {@link RequestContext} carries, and the only one this module
 * issues rather than merely forwarding: a request with no identifier is given one at the
 * first V31 service it reaches, because a trace that starts halfway through is not much
 * of a trace.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class GrpcRequestId {

	/**
	 * What it travels under, in gRPC metadata and in HTTP headers alike. Lower case
	 * because gRPC rejects a metadata key that is not, and HTTP header names are matched
	 * case-insensitively so the same spelling works for both.
	 */
	public static final String HEADER_NAME = "x-request-id";

	/**
	 * Where the identifier travels on a gRPC call.
	 */
	public static final Metadata.Key<String> METADATA_KEY = Metadata.Key.of(HEADER_NAME,
			Metadata.ASCII_STRING_MARSHALLER);

	/**
	 * Where it lives while a gRPC call is being served.
	 */
	public static final Context.Key<String> CONTEXT_KEY = Context.key("v31-request-id");

	/**
	 * Key the identifier is logged under, referenced from the logging pattern as
	 * {@code %X{requestId}}.
	 */
	public static final String MDC_KEY = "requestId";

	/**
	 * What an identifier arriving from outside has to look like to be echoed.
	 * <p>
	 * It ends up in a metadata value, in a response header, and in every log line of the
	 * request. An unchecked one would let a caller forge log entries by embedding
	 * newlines, or push something through the metadata encoder that has no business being
	 * there.
	 */
	private static final Pattern ACCEPTED = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	private GrpcRequestId() {
	}

	/**
	 * Return the identifier of the request being served.
	 * @return the identifier, or {@code null} when there is nothing to carry
	 */
	public static String current() {
		String fromContext = CONTEXT_KEY.get();
		if (fromContext != null) {
			return fromContext;
		}
		String fromRequest = RequestContext.get(HEADER_NAME);
		return (fromRequest != null) ? fromRequest : MDC.get(MDC_KEY);
	}

	/**
	 * Return the identifier to serve a request under, keeping the caller's only when it
	 * is safe to echo.
	 * @param supplied the value that arrived, which may be {@code null}
	 * @return the identifier for this request
	 */
	public static String resolve(String supplied) {
		if (supplied != null && ACCEPTED.matcher(supplied).matches()) {
			return supplied;
		}
		return Uuids.timeOrdered().toString();
	}

}

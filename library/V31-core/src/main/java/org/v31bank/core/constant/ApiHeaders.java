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

package org.v31bank.core.constant;

/**
 * Names of the HTTP headers the platform reads and forwards, kept in one place so that
 * the filter setting a header and the client sending it cannot drift apart over a
 * spelling.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class ApiHeaders {

	/**
	 * Identifier correlating everything done while handling one request: the log lines it
	 * wrote, the calls it made downstream, and the {@code traceId} on the response.
	 * Accepted from the caller when present so a trace survives across service
	 * boundaries, and generated otherwise.
	 */
	public static final String REQUEST_ID = "X-Request-Id";

	/**
	 * Key making a write safe to retry. A caller that never learns the outcome of a
	 * request — a timeout, a dropped connection — repeats it with the same key, and the
	 * service replays the original response instead of moving the money a second time.
	 * <p>
	 * Named after the header established by payment providers and specified in
	 * {@code draft-ietf-httpapi-idempotency-key-header}.
	 */
	public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

	private ApiHeaders() {
	}

}

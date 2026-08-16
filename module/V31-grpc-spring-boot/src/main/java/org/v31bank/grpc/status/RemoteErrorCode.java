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

package org.v31bank.grpc.status;

import java.util.Objects;

import org.v31bank.core.response.ErrorCode;

/**
 * An {@link ErrorCode} that came back from another service.
 * <p>
 * The caller cannot hold the enum constant the far side threw — that enum lives in the
 * callee's jar, and putting it on the caller's classpath is exactly the coupling a
 * service boundary exists to avoid. What crosses the wire is the code as text, and this
 * is what it becomes on arrival: an {@code ErrorCode} like any other, so a caller writes
 * the same {@code catch} whether the failure came from a method call or from three
 * services away.
 * <p>
 * Compare it by {@link #code()}, never by identity. {@code CUSTOMER_NOT_FOUND} arriving
 * over gRPC is a different object from the constant that produced it.
 *
 * @param code the code the far side reported
 * @param defaultMessage the message it reported with it
 * @param httpStatus the status standing for the gRPC code the call failed with
 * @author Xander Wang
 * @since 0.2.0
 */
public record RemoteErrorCode(String code, String defaultMessage, int httpStatus) implements ErrorCode {

	public RemoteErrorCode {
		Objects.requireNonNull(code, "code must not be null");
	}

}

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

import io.grpc.Metadata;
import io.grpc.Status;

import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.ErrorCode;

/**
 * Carries a platform {@link ErrorCode} across a gRPC call and back.
 *
 * <h2>Why the status code is not enough</h2>
 *
 * gRPC has sixteen status codes. This platform has as many error codes as it has ways to
 * refuse a request, and a caller that has to tell a duplicate code from a node that still
 * has children cannot do it from {@code ALREADY_EXISTS} alone — the same problem the REST
 * envelope solves by carrying a code beside the HTTP status. So the status code is the
 * coarse signal, understood by anything that speaks gRPC, and the exact code travels in a
 * trailer beside it.
 *
 * <h2>What goes where</h2>
 *
 * <ul>
 * <li>the status code — for load balancers, retry policies, and metrics, none of which
 * know this platform's codes</li>
 * <li>the status description — the message, which is written for whoever is waiting on
 * the call</li>
 * <li>{@link #ERROR_CODE} — the exact code, for the caller to branch on</li>
 * </ul>
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class GrpcStatuses {

	/**
	 * Trailer carrying the exact {@link ErrorCode#code()} a call was refused with.
	 * <p>
	 * Lower case because gRPC rejects a metadata key that is not, and prefixed so that it
	 * cannot collide with a key some library adds.
	 */
	public static final Metadata.Key<String> ERROR_CODE = Metadata.Key.of("v31-error-code",
			Metadata.ASCII_STRING_MARSHALLER);

	private GrpcStatuses() {
	}

	/**
	 * Return the status code that says roughly what an error code means.
	 * <p>
	 * The mapping goes through the HTTP status the code already declares, so a failure
	 * reported over REST and the same failure reported over gRPC agree — a service does
	 * not have to decide twice what {@code NOT_FOUND} means. It follows the
	 * correspondence Google publishes between HTTP statuses and the canonical codes.
	 * <p>
	 * It is lossy in the direction of gRPC: several distinct refusals share
	 * {@code ALREADY_EXISTS}. That is what {@link #ERROR_CODE} is for.
	 * @param errorCode the code the request was refused with
	 * @return the status code to answer with
	 */
	public static Status.Code statusCodeFor(ErrorCode errorCode) {
		return switch (errorCode.httpStatus()) {
			case 400 -> Status.Code.INVALID_ARGUMENT;
			case 401 -> Status.Code.UNAUTHENTICATED;
			case 403 -> Status.Code.PERMISSION_DENIED;
			case 404 -> Status.Code.NOT_FOUND;
			case 405, 501 -> Status.Code.UNIMPLEMENTED;
			case 409 -> Status.Code.ALREADY_EXISTS;
			case 422 -> Status.Code.FAILED_PRECONDITION;
			case 429 -> Status.Code.RESOURCE_EXHAUSTED;
			case 503 -> Status.Code.UNAVAILABLE;
			case 504 -> Status.Code.DEADLINE_EXCEEDED;
			default -> Status.Code.INTERNAL;
		};
	}

	/**
	 * Return the HTTP status that says roughly what a gRPC status means, for a failure
	 * that arrived without one of this platform's codes on it.
	 * <p>
	 * Needed when the far side is not a V31 service — a proxy that ran out of capacity, a
	 * mesh that could not route the call — and there is nothing to read from a trailer.
	 * @param code the status the call failed with
	 * @return the HTTP status standing for it
	 */
	public static int httpStatusFor(Status.Code code) {
		return switch (code) {
			case INVALID_ARGUMENT, OUT_OF_RANGE -> 400;
			case UNAUTHENTICATED -> 401;
			case PERMISSION_DENIED -> 403;
			case NOT_FOUND -> 404;
			case UNIMPLEMENTED -> 405;
			case ALREADY_EXISTS, ABORTED -> 409;
			case FAILED_PRECONDITION -> 422;
			case RESOURCE_EXHAUSTED -> 429;
			case UNAVAILABLE -> 503;
			case DEADLINE_EXCEEDED -> 504;
			default -> 500;
		};
	}

	/**
	 * Return the common code standing for a gRPC status, for the same case: a failure
	 * from something that is not a V31 service, which still has to reach the caller as an
	 * {@link ErrorCode}.
	 * @param code the status the call failed with
	 * @return the closest common code
	 */
	public static ErrorCode commonErrorCodeFor(Status.Code code) {
		return switch (code) {
			case INVALID_ARGUMENT, OUT_OF_RANGE -> CommonErrorCode.VALIDATION_FAILED;
			case UNAUTHENTICATED -> CommonErrorCode.UNAUTHENTICATED;
			case PERMISSION_DENIED -> CommonErrorCode.FORBIDDEN;
			case NOT_FOUND -> CommonErrorCode.NOT_FOUND;
			case ALREADY_EXISTS, ABORTED -> CommonErrorCode.CONFLICT;
			case FAILED_PRECONDITION -> CommonErrorCode.UNPROCESSABLE;
			case RESOURCE_EXHAUSTED -> CommonErrorCode.RATE_LIMITED;
			case UNAVAILABLE -> CommonErrorCode.DEPENDENCY_UNAVAILABLE;
			case DEADLINE_EXCEEDED -> CommonErrorCode.DEPENDENCY_TIMEOUT;
			default -> CommonErrorCode.INTERNAL_ERROR;
		};
	}

}

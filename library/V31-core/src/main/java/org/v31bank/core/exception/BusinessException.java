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

package org.v31bank.core.exception;

import java.io.Serial;
import java.util.Objects;

import org.v31bank.core.response.ErrorCode;

/**
 * Thrown when a request is refused for a reason the caller is meant to see.
 * <p>
 * The {@link ErrorCode} it carries is what lets a single exception handler answer every
 * such failure: the code supplies the HTTP status and the wire code, so a service reports
 * a new failure by adding an enum constant rather than an exception class and a handler
 * method.
 * <p>
 * Reserved for outcomes that are part of the domain — a duplicate code, a beneficiary
 * that does not exist, a limit that was exceeded. A bug or a broken dependency is not one
 * of these: let it propagate so the handler reports {@code INTERNAL_ERROR} and logs the
 * cause, instead of dressing it up as a business decision.
 * <p>
 * Where a use case has several distinct outcomes that the caller must tell apart and none
 * of them is exceptional, a sealed result type such as {@code CustomerCategoryResult}
 * says so more plainly than throwing. This is for the failures that abort the work rather
 * than the ones it returns.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class BusinessException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final ErrorCode errorCode;

	/**
	 * Create an exception reporting the code's own message.
	 * @param errorCode the reason the request was refused
	 */
	public BusinessException(ErrorCode errorCode) {
		this(errorCode, Objects.requireNonNull(errorCode, "errorCode must not be null").defaultMessage());
	}

	/**
	 * Create an exception with a message describing this occurrence, for example naming
	 * the identifier that was not found.
	 * <p>
	 * The message is reported to the caller, so it must not carry anything the caller is
	 * not entitled to see.
	 * @param errorCode the reason the request was refused
	 * @param message the message to report
	 */
	public BusinessException(ErrorCode errorCode, String message) {
		this(errorCode, message, null);
	}

	/**
	 * Create an exception with a message and the failure it was translated from, such as
	 * a constraint violation recognised as a duplicate.
	 * @param errorCode the reason the request was refused
	 * @param message the message to report
	 * @param cause the underlying failure, kept for the logs
	 */
	public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
	}

	/**
	 * Return the reason the request was refused.
	 * @return the error code
	 */
	public ErrorCode getErrorCode() {
		return this.errorCode;
	}

}

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

package org.v31bank.web.advice;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import org.v31bank.core.constant.ApiHeaders;
import org.v31bank.core.exception.BusinessException;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.ErrorCode;
import org.v31bank.core.response.FieldViolation;

/**
 * Answers every failure that escapes a controller with the platform's response envelope.
 * <p>
 * Without it a service answers its failures in two different shapes: {@link ApiResponse}
 * when a controller returned one, and whatever Spring Boot's default error page produces
 * when an exception got out. A client cannot parse both, so it parses the one it was told
 * about and breaks on the other — at precisely the moment something has already gone
 * wrong.
 *
 * <h2>Why this extends {@link ResponseEntityExceptionHandler}</h2>
 *
 * Spring raises its own exceptions long before application code runs: a path variable
 * that will not parse, a body that is not JSON, a method the route does not accept. An
 * advice that only catches {@link Exception} answers all of them with {@code 500}, which
 * is both wrong — the caller sent a bad request — and, to anything watching the error
 * rate, alarming. Extending the framework's handler keeps the status it already chose and
 * replaces only the body.
 *
 * <h2>What is not disclosed</h2>
 *
 * An unrecognised failure is reported as {@link CommonErrorCode#INTERNAL_ERROR} with the
 * code's own message. The exception's message routinely names a table, a constraint or a
 * host, and none of that is the caller's to see. It goes to the log, where the
 * {@code traceId} on the response leads.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class ApiResponseExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * Key the request identifier is logged under. Matching the one the gRPC module
	 * populates, by convention rather than by a dependency: this module has no business
	 * requiring that one to be present, and reads whatever put a value there.
	 */
	private static final String REQUEST_ID_MDC_KEY = "requestId";

	private static final Logger logger = LoggerFactory.getLogger(ApiResponseExceptionHandler.class);

	/**
	 * Answer a request the application deliberately refused, keeping the code it
	 * reported.
	 * <p>
	 * This is the one case where the exception's message is sent on: a
	 * {@link BusinessException} is raised by code that chose both the code and the
	 * wording for the caller.
	 * @param ex the refusal
	 * @param request the request being served
	 * @return the response to send
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, WebRequest request) {
		ErrorCode errorCode = ex.getErrorCode();
		logger.debug("Refused with {}", errorCode.code(), ex);
		return ResponseEntity.status(statusOf(errorCode))
			.body(ApiResponse.<Void>error(errorCode, ex.getMessage()).withTraceId(traceId(request)));
	}

	/**
	 * Answer anything unrecognised without saying what it was.
	 * @param ex the failure
	 * @param request the request being served
	 * @return the response to send
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex, WebRequest request) {
		logger.error("Unhandled failure", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponse.<Void>error(CommonErrorCode.INTERNAL_ERROR).withTraceId(traceId(request)));
	}

	/**
	 * Report a rejected body field by field, so the caller is told which fields to fix
	 * rather than only that something was wrong.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> violations = ex.getBindingResult()
			.getAllErrors()
			.stream()
			.map(ApiResponseExceptionHandler::asViolation)
			.toList();
		logger.debug("Rejected {} field(s)", violations.size());
		return handleExceptionInternal(ex, ApiResponse.invalid(violations), headers, HttpStatus.BAD_REQUEST, request);
	}

	/**
	 * Replace the {@code ProblemDetail} Spring would otherwise send with the platform's
	 * envelope, leaving the status it chose alone.
	 * <p>
	 * Only the kind of failure and the status are logged. The message of an exception
	 * raised here quotes what arrived — a validation failure names every rejected value,
	 * an unreadable body quotes the body — and these endpoints are sent names, e-mail
	 * addresses and account identifiers. Logging it would put that in the log file of
	 * every rejected request, which is the most common kind there is.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		logger.debug("Rejected with {} ({})", statusCode, ex.getClass().getSimpleName());
		ApiResponse<?> envelope = (body instanceof ApiResponse<?> supplied) ? supplied
				: ApiResponse.error(errorCodeFor(statusCode));
		return super.handleExceptionInternal(ex, envelope.withTraceId(traceId(request)), headers, statusCode, request);
	}

	/**
	 * Describe one rejected field.
	 * <p>
	 * The rejected value is deliberately left out. Validation failures are logged and
	 * echoed, and the values reaching these endpoints include names, e-mail addresses and
	 * account identifiers.
	 * @param error the rejection Spring reported
	 * @return the violation to report
	 */
	private static FieldViolation asViolation(ObjectError error) {
		String field = (error instanceof FieldError fieldError) ? fieldError.getField() : error.getObjectName();
		String message = (error.getDefaultMessage() != null) ? error.getDefaultMessage() : "is invalid";
		return new FieldViolation(field, message);
	}

	/**
	 * Return the identifier correlating this response with the server logs of the same
	 * request.
	 * @param request the request being served
	 * @return the identifier, or {@code null} when the request is not carrying one
	 */
	private static String traceId(WebRequest request) {
		String fromHeader = request.getHeader(ApiHeaders.REQUEST_ID);
		return (fromHeader != null) ? fromHeader : MDC.get(REQUEST_ID_MDC_KEY);
	}

	/**
	 * Place a refusal, defaulting to what an {@link ErrorCode} declares when the code
	 * came from a service whose enum this build does not have.
	 * @param errorCode the code the failure carried
	 * @return the status to answer with
	 */
	private static int statusOf(ErrorCode errorCode) {
		int status = errorCode.httpStatus();
		return (status >= 100 && status < 600) ? status : CommonErrorCode.UNPROCESSABLE.httpStatus();
	}

	/**
	 * Map the status Spring selected onto the code that says the same thing, so that a
	 * caller branching on {@code code} does not have to read the status for the failures
	 * the framework reports.
	 * <p>
	 * A method that is not allowed and a body that cannot be read fall through to the
	 * client-error default: {@link CommonErrorCode} has no member for either, and the
	 * status Spring chose already says which it was.
	 * @param status the status Spring chose
	 * @return the matching error code
	 */
	private static ErrorCode errorCodeFor(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> CommonErrorCode.VALIDATION_FAILED;
			case 401 -> CommonErrorCode.UNAUTHENTICATED;
			case 403 -> CommonErrorCode.FORBIDDEN;
			case 404 -> CommonErrorCode.NOT_FOUND;
			case 409 -> CommonErrorCode.CONFLICT;
			case 422 -> CommonErrorCode.UNPROCESSABLE;
			case 429 -> CommonErrorCode.RATE_LIMITED;
			default -> status.is4xxClientError() ? CommonErrorCode.VALIDATION_FAILED : CommonErrorCode.INTERNAL_ERROR;
		};
	}

}

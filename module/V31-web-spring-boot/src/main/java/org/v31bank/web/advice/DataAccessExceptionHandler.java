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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;

/**
 * Reports a write the database refused as the caller's conflict rather than the server's
 * fault.
 * <p>
 * Two requests creating the same customer at the same moment both pass the uniqueness
 * check and one loses at the index. That is a {@code 409} — the loser can read what is
 * there and decide — but left alone it surfaces as a {@code 500}, which tells the caller
 * to retry a request that will never succeed and tells the on-call engineer to look for a
 * fault that does not exist.
 * <p>
 * Ordered ahead of {@link ApiResponseExceptionHandler} so that its {@link Exception}
 * handler does not claim these first. Nothing of the failure is disclosed: the message
 * names the constraint, the table and often the conflicting value.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 200)
public class DataAccessExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(DataAccessExceptionHandler.class);

	/**
	 * Answer a write that lost at a constraint.
	 * @param ex the refusal
	 * @return the response to send
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		logger.warn("Write refused by the database", ex);
		return ResponseEntity.status(CommonErrorCode.CONFLICT.httpStatus())
			.body(ApiResponse.error(CommonErrorCode.CONFLICT));
	}

	/**
	 * Answer a write that lost a race for the same row.
	 * <p>
	 * Distinct from the above in what the caller should do: the row moved under this
	 * request, so re-reading and retrying is the correct response rather than a futile
	 * one.
	 * @param ex the failure
	 * @return the response to send
	 */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
		logger.debug("Concurrent modification", ex);
		return ResponseEntity.status(CommonErrorCode.CONFLICT.httpStatus())
			.body(ApiResponse.error(CommonErrorCode.CONFLICT, "The record changed while this request was in flight"));
	}

}

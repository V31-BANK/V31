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

package org.v31bank.transfer.presentation.controller.v1;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.ErrorCode;
import org.v31bank.core.response.PageResponse;
import org.v31bank.data.jpa.domain.PageResult;
import org.v31bank.transfer.application.dto.TransferLimitPageQuery;
import org.v31bank.transfer.application.port.in.TransferLimitUseCase;
import org.v31bank.transfer.domain.model.TransferLimit;
import org.v31bank.transfer.presentation.dto.TransferLimitRequest;
import org.v31bank.transfer.presentation.dto.TransferLimitResponse;

/**
 * REST endpoints for managing transfer limits.
 * <p>
 * Commands come back from the use case as an {@link ApiResponse} already carrying the
 * verdict, so this layer converts the payload to the wire record and puts the matching
 * status on the response.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestController
@RequestMapping(TransferLimitController.PATH)
public class TransferLimitController {

	static final String PATH = "/api/v1/transfer-limits";

	/**
	 * Status for a code this service does not recognise, matching the default an
	 * {@link ErrorCode} declares.
	 */
	private static final int UNRECOGNISED_CODE_STATUS = HttpStatus.UNPROCESSABLE_CONTENT.value();

	private final TransferLimitUseCase transferLimitInputPort;

	public TransferLimitController(TransferLimitUseCase transferLimitInputPort) {
		this.transferLimitInputPort = transferLimitInputPort;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<TransferLimitResponse>> create(@Valid @RequestBody TransferLimitRequest request) {
		ApiResponse<TransferLimit> result = this.transferLimitInputPort.create(request.code(), request.name(),
				request.dailyMax());
		if (!result.success()) {
			return toResponseEntity(result);
		}
		return ResponseEntity.created(URI.create(PATH + "/" + result.data().getId()))
			.body(result.map(TransferLimitResponse::from));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<TransferLimitResponse>> get(@PathVariable UUID id) {
		return this.transferLimitInputPort.get(id)
			.map((transferLimit) -> ResponseEntity.ok(ApiResponse.ok(TransferLimitResponse.from(transferLimit))))
			.orElseGet(() -> error(CommonErrorCode.NOT_FOUND, "No transfer limit exists with id " + id));
	}

	/**
	 * Return a page of records, newest first.
	 * @param query the filters and the pagination request
	 * @return the page of matching records
	 */
	@GetMapping
	public ApiResponse<PageResponse<TransferLimitResponse>> page(TransferLimitPageQuery query) {
		PageResult<TransferLimitResponse> page = this.transferLimitInputPort.page(query)
			.map(TransferLimitResponse::from);
		return ApiResponse
			.ok(PageResponse.of(page.getRecords(), page.getTotal(), page.getPageNumber(), page.getPageSize()));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<TransferLimitResponse>> update(@PathVariable UUID id,
			@Valid @RequestBody TransferLimitRequest request) {
		return toResponseEntity(this.transferLimitInputPort.update(id, request.code(), request.name(),
				request.dailyMax(), request.status()));
	}

	/**
	 * Answer a delete with the envelope carrying the record that was removed, rather than
	 * a bare {@code 204}, so that this endpoint is parsed like every other one.
	 * @param id the record to delete
	 * @return the response to send
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<TransferLimitResponse>> delete(@PathVariable UUID id) {
		return toResponseEntity(this.transferLimitInputPort.delete(id));
	}

	private static ResponseEntity<ApiResponse<TransferLimitResponse>> toResponseEntity(
			ApiResponse<TransferLimit> result) {
		return ResponseEntity.status(statusOf(result)).body(result.map(TransferLimitResponse::from));
	}

	/**
	 * Recover the HTTP status belonging to an outcome. The envelope carries the code as
	 * text, since that is what goes on the wire, so the status has to be looked back up
	 * rather than read off it.
	 * @param result the outcome to place
	 * @return the status to answer with
	 */
	private static int statusOf(ApiResponse<?> result) {
		if (result.success()) {
			return HttpStatus.OK.value();
		}
		return CommonErrorCode.find(result.code()).map(ErrorCode::httpStatus).orElse(UNRECOGNISED_CODE_STATUS);
	}

	private static <T> ResponseEntity<ApiResponse<T>> error(ErrorCode errorCode, String message) {
		return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.error(errorCode, message));
	}

}

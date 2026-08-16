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

package org.v31bank.risk.presentation.controller.v1;

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
import org.v31bank.risk.application.dto.RiskRulePageQuery;
import org.v31bank.risk.application.port.in.RiskRuleUseCase;
import org.v31bank.risk.domain.model.RiskRule;
import org.v31bank.risk.presentation.dto.RiskRuleRequest;
import org.v31bank.risk.presentation.dto.RiskRuleResponse;

/**
 * REST endpoints for managing risk rules.
 * <p>
 * Commands come back from the use case as an {@link ApiResponse} already carrying the
 * verdict, so this layer converts the payload to the wire record and puts the matching
 * status on the response.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestController
@RequestMapping(RiskRuleController.PATH)
public class RiskRuleController {

	static final String PATH = "/api/v1/risk-rules";

	/**
	 * Status for a code this service does not recognise, matching the default an
	 * {@link ErrorCode} declares.
	 */
	private static final int UNRECOGNISED_CODE_STATUS = HttpStatus.UNPROCESSABLE_CONTENT.value();

	private final RiskRuleUseCase riskRuleInputPort;

	public RiskRuleController(RiskRuleUseCase riskRuleInputPort) {
		this.riskRuleInputPort = riskRuleInputPort;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<RiskRuleResponse>> create(@Valid @RequestBody RiskRuleRequest request) {
		ApiResponse<RiskRule> result = this.riskRuleInputPort.create(request.code(), request.name(),
				request.severity());
		if (!result.success()) {
			return toResponseEntity(result);
		}
		return ResponseEntity.created(URI.create(PATH + "/" + result.data().getId()))
			.body(result.map(RiskRuleResponse::from));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<RiskRuleResponse>> get(@PathVariable UUID id) {
		return this.riskRuleInputPort.get(id)
			.map((riskRule) -> ResponseEntity.ok(ApiResponse.ok(RiskRuleResponse.from(riskRule))))
			.orElseGet(() -> error(CommonErrorCode.NOT_FOUND, "No risk rule exists with id " + id));
	}

	/**
	 * Return a page of records, newest first.
	 * @param query the filters and the pagination request
	 * @return the page of matching records
	 */
	@GetMapping
	public ApiResponse<PageResponse<RiskRuleResponse>> page(RiskRulePageQuery query) {
		PageResult<RiskRuleResponse> page = this.riskRuleInputPort.page(query).map(RiskRuleResponse::from);
		return ApiResponse
			.ok(PageResponse.of(page.getRecords(), page.getTotal(), page.getPageNumber(), page.getPageSize()));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<RiskRuleResponse>> update(@PathVariable UUID id,
			@Valid @RequestBody RiskRuleRequest request) {
		return toResponseEntity(this.riskRuleInputPort.update(id, request.code(), request.name(), request.severity(),
				request.status()));
	}

	/**
	 * Answer a delete with the envelope carrying the record that was removed, rather than
	 * a bare {@code 204}, so that this endpoint is parsed like every other one.
	 * @param id the record to delete
	 * @return the response to send
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<RiskRuleResponse>> delete(@PathVariable UUID id) {
		return toResponseEntity(this.riskRuleInputPort.delete(id));
	}

	private static ResponseEntity<ApiResponse<RiskRuleResponse>> toResponseEntity(ApiResponse<RiskRule> result) {
		return ResponseEntity.status(statusOf(result)).body(result.map(RiskRuleResponse::from));
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

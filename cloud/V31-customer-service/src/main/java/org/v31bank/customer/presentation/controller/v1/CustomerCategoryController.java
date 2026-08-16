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

package org.v31bank.customer.presentation.controller.v1;

import java.net.URI;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.ErrorCode;
import org.v31bank.core.response.PageResponse;
import org.v31bank.customer.application.dto.CustomerCategoryPageQuery;
import org.v31bank.customer.application.port.in.CustomerCategoryUseCase;
import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.customer.domain.model.CustomerCategory;
import org.v31bank.customer.presentation.dto.CustomerCategoryRequest;
import org.v31bank.customer.presentation.dto.CustomerCategoryResponse;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * REST endpoints for managing the customer category hierarchy.
 * <p>
 * Commands come back from the use case as an {@link ApiResponse} already carrying the
 * verdict, so this layer converts the payload from the domain model to the wire record
 * and puts the matching status on the response. It does not decide the outcome, and it
 * does not restate it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestController
@RequestMapping(CustomerCategoryController.PATH)
public class CustomerCategoryController {

	static final String PATH = "/api/v1/customer-categories";

	/**
	 * Status for a code this service does not recognise, matching the default an
	 * {@link ErrorCode} declares: the request was well formed and a rule refused it.
	 */
	private static final int UNRECOGNISED_CODE_STATUS = HttpStatus.UNPROCESSABLE_CONTENT.value();

	private final CustomerCategoryUseCase customerCategoryInputPort;

	public CustomerCategoryController(CustomerCategoryUseCase customerCategoryInputPort) {
		this.customerCategoryInputPort = customerCategoryInputPort;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CustomerCategoryResponse>> create(
			@Valid @RequestBody CustomerCategoryRequest request) {
		ApiResponse<CustomerCategory> result = this.customerCategoryInputPort.create(request.code(), request.name(),
				request.parentId(), request.sortOrder(), request.status());
		if (!result.success()) {
			return toResponseEntity(result);
		}
		return ResponseEntity.created(URI.create(PATH + "/" + result.data().getId()))
			.body(result.map(CustomerCategoryResponse::from));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<CustomerCategoryResponse>> get(@PathVariable UUID id) {
		return this.customerCategoryInputPort.get(id)
			.map((category) -> ResponseEntity.ok(ApiResponse.ok(CustomerCategoryResponse.from(category))))
			.orElseGet(() -> error(CommonErrorCode.NOT_FOUND, "No customer category exists with id " + id));
	}

	/**
	 * Return a flat page of categories across the hierarchy. Pass {@code rootOnly} or
	 * {@code parentId} to page through a single level.
	 * @param query the filters and the pagination request
	 * @return the page of matching categories
	 */
	@GetMapping
	public ApiResponse<PageResponse<CustomerCategoryResponse>> page(CustomerCategoryPageQuery query) {
		PageResult<CustomerCategoryResponse> page = this.customerCategoryInputPort.page(query)
			.map(CustomerCategoryResponse::from);
		return ApiResponse
			.ok(PageResponse.of(page.getRecords(), page.getTotal(), page.getPageNumber(), page.getPageSize()));
	}

	/**
	 * Return the hierarchy as nested nodes. Not paginated: a page of a tree would cut
	 * arbitrary branches, so callers that need pagination use the flat endpoint with
	 * {@code parentId} and expand one level at a time.
	 * @param rootId the subtree to return, or {@code null} for every root
	 * @param status status to match, or {@code null} for no filter
	 * @return the root nodes, with descendants attached
	 */
	@GetMapping("/tree")
	public ApiResponse<List<CustomerCategoryResponse>> tree(@RequestParam(required = false) UUID rootId,
			@RequestParam(required = false) CustomerCategoryStatus status) {
		return ApiResponse.ok(this.customerCategoryInputPort.tree(rootId, status)
			.stream()
			.map(CustomerCategoryResponse::fromTree)
			.toList());
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<CustomerCategoryResponse>> update(@PathVariable UUID id,
			@Valid @RequestBody CustomerCategoryRequest request) {
		return toResponseEntity(this.customerCategoryInputPort.update(id, request.code(), request.name(),
				request.parentId(), request.sortOrder(), request.status()));
	}

	/**
	 * Answer a delete with the envelope carrying the node that was removed, rather than a
	 * bare {@code 204}, so that this endpoint is parsed like every other one.
	 * @param id the category to delete
	 * @return the response to send
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<CustomerCategoryResponse>> delete(@PathVariable UUID id) {
		return toResponseEntity(this.customerCategoryInputPort.delete(id));
	}

	/**
	 * Send a command outcome, converting its payload to the wire record and putting the
	 * status that belongs with its code on the response.
	 * @param result the outcome the use case reported
	 * @return the response to send
	 */
	private static ResponseEntity<ApiResponse<CustomerCategoryResponse>> toResponseEntity(
			ApiResponse<CustomerCategory> result) {
		return ResponseEntity.status(statusOf(result)).body(result.map(CustomerCategoryResponse::from));
	}

	/**
	 * Recover the HTTP status belonging to an outcome.
	 * <p>
	 * The envelope carries the code as text, since that is what goes on the wire, so the
	 * status has to be looked back up rather than read off it. A code from outside
	 * {@link CommonErrorCode} falls back to the default an {@link ErrorCode} declares.
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

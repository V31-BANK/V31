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
import java.util.UUID;

import jakarta.validation.Valid;
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
import org.v31bank.customer.application.dto.CustomerPageQuery;
import org.v31bank.customer.application.port.in.CustomerUseCase;
import org.v31bank.customer.domain.model.Customer;
import org.v31bank.customer.presentation.dto.CustomerRequest;
import org.v31bank.customer.presentation.dto.CustomerResponse;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * REST endpoints for managing customers.
 * <p>
 * Every endpoint answers with an {@link ApiResponse}, so a caller parses one shape
 * whether the request succeeded or failed. The HTTP status keeps its usual meaning
 * alongside it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestController
@RequestMapping(CustomerController.PATH)
public class CustomerController {

	static final String PATH = "/api/v1/customers";

	private final CustomerUseCase customerInputPort;

	public CustomerController(CustomerUseCase customerInputPort) {
		this.customerInputPort = customerInputPort;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
		Customer customer = this.customerInputPort.create(request.email(), request.fullName());
		return ResponseEntity.created(URI.create(PATH + "/" + customer.getId()))
			.body(ApiResponse.ok(CustomerResponse.from(customer)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<CustomerResponse>> get(@PathVariable UUID id) {
		return this.customerInputPort.get(id)
			.map((customer) -> ResponseEntity.ok(ApiResponse.ok(CustomerResponse.from(customer))))
			.orElseGet(() -> notFound(id));
	}

	@GetMapping
	public ApiResponse<PageResponse<CustomerResponse>> page(CustomerPageQuery query) {
		PageResult<CustomerResponse> page = this.customerInputPort.page(query).map(CustomerResponse::from);
		return ApiResponse
			.ok(PageResponse.of(page.getRecords(), page.getTotal(), page.getPageNumber(), page.getPageSize()));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<CustomerResponse>> update(@PathVariable UUID id,
			@Valid @RequestBody CustomerRequest request) {
		return this.customerInputPort.update(id, request.email(), request.fullName(), request.status())
			.map((customer) -> ResponseEntity.ok(ApiResponse.ok(CustomerResponse.from(customer))))
			.orElseGet(() -> notFound(id));
	}

	/**
	 * Answer a delete with the envelope and no payload rather than a bare {@code 204}, so
	 * that this endpoint is parsed like every other one.
	 * @param id the customer to delete
	 * @return the response to send
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
		return this.customerInputPort.delete(id) ? ResponseEntity.ok(ApiResponse.ok()) : notFound(id);
	}

	private static <T> ResponseEntity<ApiResponse<T>> notFound(UUID id) {
		return error(CommonErrorCode.NOT_FOUND, "No customer exists with id " + id);
	}

	/**
	 * Report a failure, taking the status from the code so that the two cannot disagree.
	 * @param errorCode the reason the request failed
	 * @param message the message describing this occurrence
	 * @param <T> the type this endpoint returns when it succeeds
	 * @return the response to send
	 */
	private static <T> ResponseEntity<ApiResponse<T>> error(ErrorCode errorCode, String message) {
		return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.error(errorCode, message));
	}

}

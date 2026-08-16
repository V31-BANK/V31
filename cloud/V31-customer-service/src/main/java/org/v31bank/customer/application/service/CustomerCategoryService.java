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

package org.v31bank.customer.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.customer.application.dto.CustomerCategoryPageQuery;
import org.v31bank.customer.application.port.in.CustomerCategoryUseCase;
import org.v31bank.customer.application.port.out.CustomerCategoryPort;
import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.customer.domain.model.CustomerCategory;
import org.v31bank.customer.domain.service.CustomerCategoryHierarchy;
import org.v31bank.data.jpa.domain.PageResult;
import org.v31bank.data.jpa.util.Trees;

/**
 * Default {@link CustomerCategoryUseCase} implementation.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Service
@Transactional
public class CustomerCategoryService implements CustomerCategoryUseCase {

	private final CustomerCategoryPort customerCategoryRepository;

	public CustomerCategoryService(CustomerCategoryPort customerCategoryRepository) {
		this.customerCategoryRepository = customerCategoryRepository;
	}

	@Override
	public ApiResponse<CustomerCategory> create(String code, String name, UUID parentId, Integer sortOrder,
			CustomerCategoryStatus status) {
		if (this.customerCategoryRepository.existsByCode(code)) {
			return ApiResponse.error(CommonErrorCode.CONFLICT,
					"Customer category code '" + code + "' is already in use");
		}
		if (parentId != null && this.customerCategoryRepository.findById(parentId).isEmpty()) {
			return ApiResponse.error(CommonErrorCode.UNPROCESSABLE,
					"No parent customer category exists with id " + parentId);
		}
		CustomerCategory category = new CustomerCategory();
		category.setCode(code);
		category.setName(name);
		category.setParentId(parentId);
		category.setSortOrder(sortOrder);
		if (status != null) {
			category.setStatus(status);
		}
		return ApiResponse.ok(this.customerCategoryRepository.save(category));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CustomerCategory> get(UUID id) {
		return this.customerCategoryRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<CustomerCategory> page(CustomerCategoryPageQuery query) {
		return this.customerCategoryRepository.findPage(query);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CustomerCategory> tree(UUID rootId, CustomerCategoryStatus status) {
		List<CustomerCategory> roots = Trees.build(this.customerCategoryRepository.findAll(status));
		if (rootId == null) {
			return roots;
		}
		return findNode(roots, rootId).map(List::of).orElseGet(List::of);
	}

	@Override
	public ApiResponse<CustomerCategory> update(UUID id, String code, String name, UUID parentId, Integer sortOrder,
			CustomerCategoryStatus status) {
		Optional<CustomerCategory> found = this.customerCategoryRepository.findById(id);
		if (found.isEmpty()) {
			return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No customer category exists with id " + id);
		}
		CustomerCategory category = found.get();
		if (!category.getCode().equals(code) && this.customerCategoryRepository.existsByCode(code)) {
			return ApiResponse.error(CommonErrorCode.CONFLICT,
					"Customer category code '" + code + "' is already in use");
		}
		if (parentId != null) {
			if (this.customerCategoryRepository.findById(parentId).isEmpty()) {
				return ApiResponse.error(CommonErrorCode.UNPROCESSABLE,
						"No parent customer category exists with id " + parentId);
			}
			if (CustomerCategoryHierarchy.createsCycle(id, parentId, this.customerCategoryRepository::findById)) {
				return ApiResponse.error(CommonErrorCode.CONFLICT, "Customer category " + id
						+ " cannot be moved under itself or one of its descendants " + parentId);
			}
		}
		category.setCode(code);
		category.setName(name);
		category.setParentId(parentId);
		category.setSortOrder(sortOrder);
		if (status != null) {
			category.setStatus(status);
		}
		return ApiResponse.ok(this.customerCategoryRepository.save(category));
	}

	@Override
	public ApiResponse<CustomerCategory> delete(UUID id) {
		Optional<CustomerCategory> found = this.customerCategoryRepository.findById(id);
		if (found.isEmpty()) {
			return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No customer category exists with id " + id);
		}
		if (this.customerCategoryRepository.existsByParentId(id)) {
			return ApiResponse.error(CommonErrorCode.CONFLICT,
					"Customer category " + id + " still has children and cannot be deleted");
		}
		CustomerCategory category = found.get();
		this.customerCategoryRepository.delete(category);
		return ApiResponse.ok(category);
	}

	/**
	 * Locate a node by identifier in an already assembled tree.
	 * @param nodes the nodes to search, along with their descendants
	 * @param id the identifier to look for
	 * @return the matching node, with its subtree attached
	 */
	private static Optional<CustomerCategory> findNode(List<CustomerCategory> nodes, UUID id) {
		for (CustomerCategory node : nodes) {
			if (id.equals(node.getId())) {
				return Optional.of(node);
			}
			Optional<CustomerCategory> found = findNode(node.getChildren(), id);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}

}

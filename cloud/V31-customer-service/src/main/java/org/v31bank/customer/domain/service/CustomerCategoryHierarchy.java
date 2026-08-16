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

package org.v31bank.customer.domain.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.v31bank.customer.domain.model.CustomerCategory;

/**
 * Structural rules of the customer category hierarchy.
 * <p>
 * Nodes are resolved through a caller-supplied lookup, which keeps these rules free of
 * any dependency on how categories are stored or fetched.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class CustomerCategoryHierarchy {

	private CustomerCategoryHierarchy() {
	}

	/**
	 * Whether attaching a node under the given parent would create a cycle.
	 * <p>
	 * Walks the ancestor chain upwards starting at {@code parentId}: reaching
	 * {@code nodeId} means the node would end up being its own ancestor. A chain that
	 * revisits a node is already corrupt and is reported as a cycle rather than followed
	 * forever.
	 * @param nodeId the node being attached
	 * @param parentId the candidate parent, or {@code null} to attach as a root
	 * @param lookup resolves a category by its identifier
	 * @return {@code true} if the attachment would create a cycle
	 */
	public static boolean createsCycle(UUID nodeId, UUID parentId, Function<UUID, Optional<CustomerCategory>> lookup) {
		Set<UUID> visited = new HashSet<>();
		UUID ancestorId = parentId;
		while (ancestorId != null) {
			if (ancestorId.equals(nodeId) || !visited.add(ancestorId)) {
				return true;
			}
			ancestorId = lookup.apply(ancestorId).map(CustomerCategory::getParentId).orElse(null);
		}
		return false;
	}

}

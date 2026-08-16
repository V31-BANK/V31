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

package org.v31bank.transfer.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.data.jpa.domain.PageResult;
import org.v31bank.transfer.application.dto.TransferLimitPageQuery;
import org.v31bank.transfer.domain.model.TransferLimit;

/**
 * Output port for {@link TransferLimit} persistence, implemented by the infrastructure
 * layer.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface TransferLimitPort {

	TransferLimit save(TransferLimit transferLimit);

	Optional<TransferLimit> findById(UUID id);

	/**
	 * Find a page of records matching the filters carried by the query.
	 * @param query the filters and the pagination request
	 * @return the page of matching records
	 */
	PageResult<TransferLimit> findPage(TransferLimitPageQuery query);

	/**
	 * Whether any record already uses the given code.
	 * @param code the value to check
	 * @return {@code true} if it is taken
	 */
	boolean existsByCode(String code);

	void delete(TransferLimit transferLimit);

}

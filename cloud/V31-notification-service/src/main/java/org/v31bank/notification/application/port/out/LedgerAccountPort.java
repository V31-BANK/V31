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

package org.v31bank.notification.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.core.response.PageResponse;
import org.v31bank.notification.application.dto.LedgerAccountSummary;

/**
 * Output port for the chart of accounts, which this service does not own.
 * <p>
 * Declared here and implemented in the infrastructure layer, exactly as a persistence
 * port is — the application layer does not know whether the accounts come from a
 * database, a gRPC call or a cache, and swapping one for another touches only the
 * adapter.
 * <p>
 * Failures arrive as {@link org.v31bank.core.exception.BusinessException}, carrying the
 * code the ledger reported. That is the point of the translation the adapter does: a
 * remote refusal reaches this layer looking like any other, and nothing here has to know
 * what a gRPC status is.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface LedgerAccountPort {

	/**
	 * Add an account to the ledger's catalogue.
	 * @param code the code the account is known by, unique in the ledger
	 * @param name the display name
	 * @param type which side of the balance sheet it belongs to
	 * @return the account as the ledger now holds it
	 */
	LedgerAccountSummary create(String code, String name, String type);

	/**
	 * Find one account.
	 * <p>
	 * An account the ledger does not have comes back empty rather than as a failure,
	 * matching every other {@code findById} in the platform. Any other refusal is raised.
	 * @param id the account to look up
	 * @return the account, or empty when the ledger has none with that identifier
	 */
	Optional<LedgerAccountSummary> findById(UUID id);

	/**
	 * Find a page of accounts, newest first.
	 * @param pageNumber the one-based page to fetch
	 * @param pageSize the page size
	 * @param code fragment matched against the code, or {@code null} for no filter
	 * @return the page of matching accounts
	 */
	PageResponse<LedgerAccountSummary> findPage(int pageNumber, int pageSize, String code);

	/**
	 * Update an account.
	 * @param id the account to update
	 * @param code the code, which must not belong to another account
	 * @param name the display name
	 * @param type which side of the balance sheet it belongs to
	 * @param status the new status, or {@code null} to leave it unchanged
	 * @return the account as the ledger now holds it
	 */
	LedgerAccountSummary update(UUID id, String code, String name, String type, String status);

	/**
	 * Remove an account.
	 * @param id the account to remove
	 * @return the account that was removed
	 */
	LedgerAccountSummary delete(UUID id);

}

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

package org.v31bank.notification.application.port.in;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.core.response.PageResponse;
import org.v31bank.notification.application.dto.LedgerAccountSummary;

/**
 * Use cases for the chart of accounts this service reads from the ledger.
 * <p>
 * Thin today, and deliberately still here: it is the seam where this service will put
 * what it actually needs — caching a catalogue that changes rarely, falling back to the
 * last known copy when the ledger is unreachable, filtering to the accounts a template
 * may name. Calling the port straight from the controller would leave nowhere for any of
 * that to go.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface LedgerAccountUseCase {

	LedgerAccountSummary create(String code, String name, String type);

	Optional<LedgerAccountSummary> get(UUID id);

	PageResponse<LedgerAccountSummary> page(int pageNumber, int pageSize, String code);

	LedgerAccountSummary update(UUID id, String code, String name, String type, String status);

	LedgerAccountSummary delete(UUID id);

}

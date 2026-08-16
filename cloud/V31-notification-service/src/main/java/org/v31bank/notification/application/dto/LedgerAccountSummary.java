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

package org.v31bank.notification.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * What this service knows about an account in the ledger's chart of accounts.
 * <p>
 * Its own type, not the ledger's domain model and not the generated message. The
 * generated types belong to the transport and change whenever the contract does; the
 * ledger's domain model is not this service's to depend on at all. What crosses into this
 * service is what it needs to render a message about an account, and nothing else.
 * <p>
 * The type and status are text rather than enums copied from the ledger. A notification
 * service puts them in a sentence; it does not branch on them, and duplicating the
 * ledger's enums here would mean redeploying this service every time the ledger adds a
 * member it never reads.
 *
 * @param id the account identifier
 * @param code the code the account is known by
 * @param name the display name
 * @param type which side of the balance sheet it belongs to
 * @param status whether it is still posted to
 * @param createdDate when it was created
 * @param lastModifiedDate when it last changed
 * @author Xander Wang
 * @since 0.2.0
 */
public record LedgerAccountSummary(UUID id, String code, String name, String type, String status, Instant createdDate,
		Instant lastModifiedDate) {

}

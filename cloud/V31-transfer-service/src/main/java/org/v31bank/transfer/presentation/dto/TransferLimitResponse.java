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

package org.v31bank.transfer.presentation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.v31bank.transfer.domain.constant.TransferLimitStatus;
import org.v31bank.transfer.domain.model.TransferLimit;

/**
 * API representation of a transfer limit.
 *
 * @param id the identifier it was issued when it was created
 * @param code the code it is known by, unique within the service
 * @param name what to call it
 * @param dailyMax the most that may move in a day
 * @param status where it is in its lifecycle
 * @param createdDate when it was created
 * @param lastModifiedDate when it was last changed
 * @author Xander Wang
 * @since 0.2.0
 */
public record TransferLimitResponse(UUID id, String code, String name, BigDecimal dailyMax, TransferLimitStatus status,
		Instant createdDate, Instant lastModifiedDate) {

	public static TransferLimitResponse from(TransferLimit transferLimit) {
		return new TransferLimitResponse(transferLimit.getId(), transferLimit.getCode(), transferLimit.getName(),
				transferLimit.getDailyMax(), transferLimit.getStatus(), transferLimit.getCreatedDate(),
				transferLimit.getLastModifiedDate());
	}

}

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

package org.v31bank.ledger.infra.grpc.adapter;

import java.time.Instant;
import java.util.UUID;

import com.google.protobuf.Timestamp;

import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.ErrorCode;
import org.v31bank.ledger.domain.constant.LedgerAccountStatus;
import org.v31bank.ledger.domain.constant.LedgerAccountType;
import org.v31bank.ledger.domain.model.LedgerAccount;

/**
 * Where the wire contract and the domain model meet.
 * <p>
 * Neither knows about the other, which is the point: the proto file can gain a field
 * without the domain changing, and the domain can be reshaped without breaking a caller
 * compiled against an older copy of the contract. Everything that would otherwise couple
 * them is spent here.
 *
 * <h2>Enums do not map by name</h2>
 *
 * Proto3 requires every enum to have a zero value, and gives it to whatever is listed
 * first unless one is reserved for "not set". This contract reserves one, so the names
 * differ from the domain's by a prefix and by that extra member. Translating by
 * {@code valueOf} would work until the day someone reorders the proto, so the mapping is
 * written out.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class LedgerAccountProtos {

	private LedgerAccountProtos() {
	}

	/**
	 * Render an account for the wire.
	 * @param account the account to render
	 * @return the message
	 */
	public static org.v31bank.ledger.api.v1.LedgerAccount toProto(LedgerAccount account) {
		org.v31bank.ledger.api.v1.LedgerAccount.Builder builder = org.v31bank.ledger.api.v1.LedgerAccount.newBuilder()
			.setId(account.getId().toString())
			.setCode(account.getCode())
			.setName(account.getName())
			.setType(toProto(account.getType()))
			.setStatus(toProto(account.getStatus()));
		if (account.getCreatedDate() != null) {
			builder.setCreatedDate(toProto(account.getCreatedDate()));
		}
		if (account.getLastModifiedDate() != null) {
			builder.setLastModifiedDate(toProto(account.getLastModifiedDate()));
		}
		return builder.build();
	}

	public static org.v31bank.ledger.api.v1.LedgerAccountType toProto(LedgerAccountType type) {
		return switch (type) {
			case ASSET -> org.v31bank.ledger.api.v1.LedgerAccountType.LEDGER_ACCOUNT_TYPE_ASSET;
			case LIABILITY -> org.v31bank.ledger.api.v1.LedgerAccountType.LEDGER_ACCOUNT_TYPE_LIABILITY;
			case EQUITY -> org.v31bank.ledger.api.v1.LedgerAccountType.LEDGER_ACCOUNT_TYPE_EQUITY;
			case REVENUE -> org.v31bank.ledger.api.v1.LedgerAccountType.LEDGER_ACCOUNT_TYPE_REVENUE;
			case EXPENSE -> org.v31bank.ledger.api.v1.LedgerAccountType.LEDGER_ACCOUNT_TYPE_EXPENSE;
		};
	}

	public static org.v31bank.ledger.api.v1.LedgerAccountStatus toProto(LedgerAccountStatus status) {
		return switch (status) {
			case ACTIVE -> org.v31bank.ledger.api.v1.LedgerAccountStatus.LEDGER_ACCOUNT_STATUS_ACTIVE;
			case CLOSED -> org.v31bank.ledger.api.v1.LedgerAccountStatus.LEDGER_ACCOUNT_STATUS_CLOSED;
		};
	}

	/**
	 * Read a type off the wire.
	 * @param type the value received
	 * @return the type, or {@code null} when the caller left it unset — which the caller
	 * is entitled to do, and which each request decides the meaning of
	 */
	public static LedgerAccountType fromProto(org.v31bank.ledger.api.v1.LedgerAccountType type) {
		return switch (type) {
			case LEDGER_ACCOUNT_TYPE_ASSET -> LedgerAccountType.ASSET;
			case LEDGER_ACCOUNT_TYPE_LIABILITY -> LedgerAccountType.LIABILITY;
			case LEDGER_ACCOUNT_TYPE_EQUITY -> LedgerAccountType.EQUITY;
			case LEDGER_ACCOUNT_TYPE_REVENUE -> LedgerAccountType.REVENUE;
			case LEDGER_ACCOUNT_TYPE_EXPENSE -> LedgerAccountType.EXPENSE;
			// Unset, or a member added by a newer contract than this build knows.
			default -> null;
		};
	}

	public static LedgerAccountStatus fromProto(org.v31bank.ledger.api.v1.LedgerAccountStatus status) {
		return switch (status) {
			case LEDGER_ACCOUNT_STATUS_ACTIVE -> LedgerAccountStatus.ACTIVE;
			case LEDGER_ACCOUNT_STATUS_CLOSED -> LedgerAccountStatus.CLOSED;
			default -> null;
		};
	}

	/**
	 * Read an identifier off the wire, refusing anything that is not one.
	 * <p>
	 * The field is a string on the wire because protobuf has no UUID, so this is the
	 * boundary where that string has to become an identifier or be rejected.
	 * @param id the value received
	 * @return the identifier
	 * @throws org.v31bank.core.exception.BusinessException if it is not a UUID
	 */
	public static UUID toUuid(String id) {
		try {
			return UUID.fromString(id);
		}
		catch (IllegalArgumentException ex) {
			throw new org.v31bank.core.exception.BusinessException(CommonErrorCode.VALIDATION_FAILED,
					"'" + id + "' is not an identifier", ex);
		}
	}

	/**
	 * Return the code a failed outcome reported, so that it can be raised as an exception
	 * the transport knows how to render.
	 * @param code the code as the envelope carried it
	 * @return the matching common code
	 */
	public static ErrorCode errorCodeOf(String code) {
		return CommonErrorCode.find(code).map(ErrorCode.class::cast).orElse(CommonErrorCode.UNPROCESSABLE);
	}

	private static Timestamp toProto(Instant instant) {
		return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
	}

}

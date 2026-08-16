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

package org.v31bank.notification.infra.grpc.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import org.v31bank.core.exception.BusinessException;
import org.v31bank.core.response.PageResponse;
import org.v31bank.grpc.client.GrpcErrors;
import org.v31bank.ledger.api.v1.CreateLedgerAccountRequest;
import org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest;
import org.v31bank.ledger.api.v1.GetLedgerAccountRequest;
import org.v31bank.ledger.api.v1.LedgerAccountServiceGrpc;
import org.v31bank.ledger.api.v1.LedgerAccountStatus;
import org.v31bank.ledger.api.v1.LedgerAccountType;
import org.v31bank.ledger.api.v1.ListLedgerAccountsRequest;
import org.v31bank.ledger.api.v1.ListLedgerAccountsResponse;
import org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest;
import org.v31bank.notification.application.dto.LedgerAccountSummary;
import org.v31bank.notification.application.port.out.LedgerAccountPort;

/**
 * {@link LedgerAccountPort} adapter backed by a gRPC call to the ledger service.
 * <p>
 * The counterpart of a persistence adapter: everything that knows the accounts come from
 * another service over a wire is confined here, and the application layer above sees a
 * port like any other.
 *
 * <h2>Failures are translated, not propagated</h2>
 *
 * Every call goes through {@link GrpcErrors}, so a refusal from the ledger arrives here
 * as a {@link BusinessException} carrying the code the ledger reported rather than as a
 * {@link StatusRuntimeException}. Without that, a status code would leak into the
 * application layer and every caller would have to know what {@code ALREADY_EXISTS}
 * means.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Component
public class LedgerAccountGrpcAdapter implements LedgerAccountPort {

	private final LedgerAccountServiceGrpc.LedgerAccountServiceBlockingStub ledger;

	public LedgerAccountGrpcAdapter(LedgerAccountServiceGrpc.LedgerAccountServiceBlockingStub ledger) {
		this.ledger = ledger;
	}

	@Override
	public LedgerAccountSummary create(String code, String name, String type) {
		return toSummary(GrpcErrors.call(() -> this.ledger.createLedgerAccount(CreateLedgerAccountRequest.newBuilder()
			.setCode(nullToEmpty(code))
			.setName(nullToEmpty(name))
			.setType(toType(type))
			.build())).getLedgerAccount());
	}

	/**
	 * Look one up, taking {@code NOT_FOUND} as an answer rather than a failure.
	 * <p>
	 * Every other refusal is raised: an account this service is not allowed to see, or a
	 * ledger that is down, is not the same as an account that does not exist, and
	 * returning empty for those would hide a problem behind a blank result.
	 */
	@Override
	public Optional<LedgerAccountSummary> findById(UUID id) {
		try {
			return Optional.of(toSummary(
					this.ledger.getLedgerAccount(GetLedgerAccountRequest.newBuilder().setId(id.toString()).build())
						.getLedgerAccount()));
		}
		catch (StatusRuntimeException ex) {
			if (ex.getStatus().getCode() == Status.Code.NOT_FOUND) {
				return Optional.empty();
			}
			throw GrpcErrors.asBusinessException(ex);
		}
	}

	@Override
	public PageResponse<LedgerAccountSummary> findPage(int pageNumber, int pageSize, String code) {
		ListLedgerAccountsRequest.Builder request = ListLedgerAccountsRequest.newBuilder()
			.setPageNumber(pageNumber)
			.setPageSize(pageSize);
		if (StringUtils.hasText(code)) {
			request.setCode(code);
		}
		ListLedgerAccountsResponse response = GrpcErrors.call(() -> this.ledger.listLedgerAccounts(request.build()));
		List<LedgerAccountSummary> records = response.getRecordsList()
			.stream()
			.map(LedgerAccountGrpcAdapter::toSummary)
			.toList();
		return PageResponse.of(records, response.getTotal(), response.getPageNumber(), response.getPageSize());
	}

	@Override
	public LedgerAccountSummary update(UUID id, String code, String name, String type, String status) {
		return toSummary(GrpcErrors.call(() -> this.ledger.updateLedgerAccount(UpdateLedgerAccountRequest.newBuilder()
			.setId(id.toString())
			.setCode(nullToEmpty(code))
			.setName(nullToEmpty(name))
			.setType(toType(type))
			.setStatus(toStatus(status))
			.build())).getLedgerAccount());
	}

	@Override
	public LedgerAccountSummary delete(UUID id) {
		return toSummary(GrpcErrors
			.call(() -> this.ledger
				.deleteLedgerAccount(DeleteLedgerAccountRequest.newBuilder().setId(id.toString()).build()))
			.getLedgerAccount());
	}

	/**
	 * Turn a message into this service's own view of an account.
	 * <p>
	 * The enum names lose the prefix the contract carries: what reaches a message
	 * template should read {@code ASSET}, not {@code LEDGER_ACCOUNT_TYPE_ASSET}.
	 * @param account the message received
	 * @return the summary
	 */
	private static LedgerAccountSummary toSummary(org.v31bank.ledger.api.v1.LedgerAccount account) {
		return new LedgerAccountSummary(UUID.fromString(account.getId()), account.getCode(), account.getName(),
				strip(account.getType().name(), "LEDGER_ACCOUNT_TYPE_"),
				strip(account.getStatus().name(), "LEDGER_ACCOUNT_STATUS_"), toInstant(account.getCreatedDate()),
				toInstant(account.getLastModifiedDate()));
	}

	/**
	 * Read a type to send, leaving it unset when there is nothing to say.
	 * <p>
	 * A value this build does not recognise is refused here rather than sent as
	 * "unspecified", which the ledger would read as "no filter" or "no type" and act on
	 * silently.
	 * @param type the value to send, which may be {@code null}
	 * @return the value for the wire
	 */
	private static LedgerAccountType toType(String type) {
		if (!StringUtils.hasText(type)) {
			return LedgerAccountType.LEDGER_ACCOUNT_TYPE_UNSPECIFIED;
		}
		LedgerAccountType value = LedgerAccountType.valueOf("LEDGER_ACCOUNT_TYPE_" + type);
		return (value != LedgerAccountType.UNRECOGNIZED) ? value : LedgerAccountType.LEDGER_ACCOUNT_TYPE_UNSPECIFIED;
	}

	private static LedgerAccountStatus toStatus(String status) {
		if (!StringUtils.hasText(status)) {
			return LedgerAccountStatus.LEDGER_ACCOUNT_STATUS_UNSPECIFIED;
		}
		LedgerAccountStatus value = LedgerAccountStatus.valueOf("LEDGER_ACCOUNT_STATUS_" + status);
		return (value != LedgerAccountStatus.UNRECOGNIZED) ? value
				: LedgerAccountStatus.LEDGER_ACCOUNT_STATUS_UNSPECIFIED;
	}

	private static String strip(String name, String prefix) {
		return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
	}

	private static Instant toInstant(Timestamp timestamp) {
		return (timestamp.getSeconds() == 0 && timestamp.getNanos() == 0) ? null
				: Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}

	private static String nullToEmpty(String value) {
		return (value != null) ? value : "";
	}

}

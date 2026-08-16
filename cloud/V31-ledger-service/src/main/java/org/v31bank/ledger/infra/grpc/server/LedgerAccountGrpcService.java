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

package org.v31bank.ledger.infra.grpc.server;

import java.util.UUID;

import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import org.v31bank.core.exception.BusinessException;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.data.jpa.domain.PageResult;
import org.v31bank.ledger.api.v1.CreateLedgerAccountRequest;
import org.v31bank.ledger.api.v1.CreateLedgerAccountResponse;
import org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest;
import org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse;
import org.v31bank.ledger.api.v1.GetLedgerAccountRequest;
import org.v31bank.ledger.api.v1.GetLedgerAccountResponse;
import org.v31bank.ledger.api.v1.LedgerAccountServiceGrpc;
import org.v31bank.ledger.api.v1.ListLedgerAccountsRequest;
import org.v31bank.ledger.api.v1.ListLedgerAccountsResponse;
import org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest;
import org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse;
import org.v31bank.ledger.application.dto.LedgerAccountPageQuery;
import org.v31bank.ledger.application.port.in.LedgerAccountUseCase;
import org.v31bank.ledger.domain.model.LedgerAccount;
import org.v31bank.ledger.infra.grpc.adapter.LedgerAccountProtos;

/**
 * The chart of accounts, served over gRPC.
 * <p>
 * A second way in to the same use cases the REST controller calls — not a second
 * implementation of them. Nothing is decided here: the request is translated, the use
 * case is asked, and the outcome is translated back.
 *
 * <h2>How a refusal becomes a status</h2>
 *
 * The use cases report an outcome rather than throwing, because that is what suits an
 * HTTP response. gRPC has no envelope: a call either returns its message or fails with a
 * status. So a refused outcome is raised as a {@link BusinessException} here, and the
 * handler the gRPC starter registers turns it into a status carrying the same code the
 * REST layer would have put in the envelope. A caller therefore sees the same
 * {@code CONFLICT} either way.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@GrpcService
public class LedgerAccountGrpcService extends LedgerAccountServiceGrpc.LedgerAccountServiceImplBase {

	private final LedgerAccountUseCase ledgerAccountInputPort;

	public LedgerAccountGrpcService(LedgerAccountUseCase ledgerAccountInputPort) {
		this.ledgerAccountInputPort = ledgerAccountInputPort;
	}

	@Override
	public void createLedgerAccount(CreateLedgerAccountRequest request,
			StreamObserver<CreateLedgerAccountResponse> observer) {
		LedgerAccount account = unwrap(this.ledgerAccountInputPort.create(request.getCode(), request.getName(),
				LedgerAccountProtos.fromProto(request.getType())));
		respond(observer,
				CreateLedgerAccountResponse.newBuilder()
					.setLedgerAccount(LedgerAccountProtos.toProto(account))
					.build());
	}

	@Override
	public void getLedgerAccount(GetLedgerAccountRequest request, StreamObserver<GetLedgerAccountResponse> observer) {
		UUID id = LedgerAccountProtos.toUuid(request.getId());
		LedgerAccount account = this.ledgerAccountInputPort.get(id)
			.orElseThrow(
					() -> new BusinessException(CommonErrorCode.NOT_FOUND, "No ledger account exists with id " + id));
		respond(observer,
				GetLedgerAccountResponse.newBuilder().setLedgerAccount(LedgerAccountProtos.toProto(account)).build());
	}

	@Override
	public void listLedgerAccounts(ListLedgerAccountsRequest request,
			StreamObserver<ListLedgerAccountsResponse> observer) {
		LedgerAccountPageQuery query = new LedgerAccountPageQuery();
		if (request.getPageNumber() > 0) {
			query.setPageNumber(request.getPageNumber());
		}
		if (request.getPageSize() > 0) {
			query.setPageSize(request.getPageSize());
		}
		if (!request.getCode().isEmpty()) {
			query.setCode(request.getCode());
		}
		query.setType(LedgerAccountProtos.fromProto(request.getType()));
		query.setStatus(LedgerAccountProtos.fromProto(request.getStatus()));
		PageResult<LedgerAccount> page = this.ledgerAccountInputPort.page(query);
		ListLedgerAccountsResponse.Builder response = ListLedgerAccountsResponse.newBuilder()
			.setTotal(page.getTotal())
			.setPageNumber(page.getPageNumber())
			.setPageSize(page.getPageSize())
			.setTotalPages(page.getTotalPages())
			.setHasNext(page.hasNext());
		page.getRecords().forEach((account) -> response.addRecords(LedgerAccountProtos.toProto(account)));
		observer.onNext(response.build());
		observer.onCompleted();
	}

	@Override
	public void updateLedgerAccount(UpdateLedgerAccountRequest request,
			StreamObserver<UpdateLedgerAccountResponse> observer) {
		LedgerAccount account = unwrap(this.ledgerAccountInputPort.update(LedgerAccountProtos.toUuid(request.getId()),
				request.getCode(), request.getName(), LedgerAccountProtos.fromProto(request.getType()),
				LedgerAccountProtos.fromProto(request.getStatus())));
		respond(observer,
				UpdateLedgerAccountResponse.newBuilder()
					.setLedgerAccount(LedgerAccountProtos.toProto(account))
					.build());
	}

	@Override
	public void deleteLedgerAccount(DeleteLedgerAccountRequest request,
			StreamObserver<DeleteLedgerAccountResponse> observer) {
		LedgerAccount account = unwrap(this.ledgerAccountInputPort.delete(LedgerAccountProtos.toUuid(request.getId())));
		respond(observer,
				DeleteLedgerAccountResponse.newBuilder()
					.setLedgerAccount(LedgerAccountProtos.toProto(account))
					.build());
	}

	/**
	 * Take the account out of a successful outcome, or raise the refusal.
	 * @param result what the use case reported
	 * @return the account
	 */
	private static LedgerAccount unwrap(ApiResponse<LedgerAccount> result) {
		if (!result.success()) {
			throw new BusinessException(LedgerAccountProtos.errorCodeOf(result.code()), result.message());
		}
		return result.data();
	}

	private static <T> void respond(StreamObserver<T> observer, T response) {
		observer.onNext(response);
		observer.onCompleted();
	}

}

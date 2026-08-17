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

package org.v31bank.ledger.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.data.jpa.domain.PageResult;
import org.v31bank.ledger.application.dto.LedgerAccountPageQuery;
import org.v31bank.ledger.application.port.in.LedgerAccountUseCase;
import org.v31bank.ledger.application.port.out.LedgerAccountPort;
import org.v31bank.ledger.domain.constant.LedgerAccountStatus;
import org.v31bank.ledger.domain.constant.LedgerAccountType;
import org.v31bank.ledger.domain.model.LedgerAccount;

/**
 * Default {@link LedgerAccountUseCase} implementation.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Service
@Transactional
public class LedgerAccountService implements LedgerAccountUseCase {

	private final LedgerAccountPort ledgerAccountRepository;

	public LedgerAccountService(LedgerAccountPort ledgerAccountRepository) {
		this.ledgerAccountRepository = ledgerAccountRepository;
	}

	@Override
	public ApiResponse<LedgerAccount> create(String code, String name, LedgerAccountType type) {
		ApiResponse<LedgerAccount> invalid = validate(code, name, type);
		if (invalid != null) {
			return invalid;
		}
		if (this.ledgerAccountRepository.existsByCode(code)) {
			return ApiResponse.error(CommonErrorCode.CONFLICT, "Code '" + code + "' is already in use");
		}
		LedgerAccount ledgerAccount = new LedgerAccount();
		ledgerAccount.setCode(code);
		ledgerAccount.setName(name);
		ledgerAccount.setType(type);
		return ApiResponse.ok(this.ledgerAccountRepository.save(ledgerAccount));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<LedgerAccount> get(UUID id) {
		return this.ledgerAccountRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<LedgerAccount> page(LedgerAccountPageQuery query) {
		return this.ledgerAccountRepository.findPage(query);
	}

	@Override
	public ApiResponse<LedgerAccount> update(UUID id, String code, String name, LedgerAccountType type,
			LedgerAccountStatus status) {
		ApiResponse<LedgerAccount> invalid = validate(code, name, type);
		if (invalid != null) {
			return invalid;
		}
		Optional<LedgerAccount> found = this.ledgerAccountRepository.findById(id);
		if (found.isEmpty()) {
			return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No ledger account exists with id " + id);
		}
		LedgerAccount ledgerAccount = found.get();
		if (!ledgerAccount.getCode().equals(code) && this.ledgerAccountRepository.existsByCode(code)) {
			return ApiResponse.error(CommonErrorCode.CONFLICT, "Code '" + code + "' is already in use");
		}
		ledgerAccount.setCode(code);
		ledgerAccount.setName(name);
		ledgerAccount.setType(type);
		if (status != null) {
			ledgerAccount.setStatus(status);
		}
		return ApiResponse.ok(this.ledgerAccountRepository.save(ledgerAccount));
	}

	/**
	 * Check what an account may not be saved without.
	 * <p>
	 * Here rather than on the request record, because the REST layer is not the only way
	 * in: a proto3 scalar has no presence, so a gRPC caller that omits a field sends an
	 * empty string or an unspecified enum instead of nothing at all. Left to the mapping,
	 * an empty code reaches a {@code not null} column that accepts it, and an unspecified
	 * type reaches one that does not — the first stored, the second an INTERNAL the
	 * caller cannot act on.
	 * @param code the code to check
	 * @param name the name to check
	 * @param type the type to check
	 * @return the refusal, or {@code null} when there is nothing to refuse
	 */
	private static ApiResponse<LedgerAccount> validate(String code, String name, LedgerAccountType type) {
		if (!StringUtils.hasText(code)) {
			return ApiResponse.error(CommonErrorCode.VALIDATION_FAILED, "Code is required");
		}
		if (code.length() > LedgerAccount.CODE_MAX_LENGTH) {
			return ApiResponse.error(CommonErrorCode.VALIDATION_FAILED,
					"Code is longer than " + LedgerAccount.CODE_MAX_LENGTH + " characters");
		}
		if (!StringUtils.hasText(name)) {
			return ApiResponse.error(CommonErrorCode.VALIDATION_FAILED, "Name is required");
		}
		if (name.length() > LedgerAccount.NAME_MAX_LENGTH) {
			return ApiResponse.error(CommonErrorCode.VALIDATION_FAILED,
					"Name is longer than " + LedgerAccount.NAME_MAX_LENGTH + " characters");
		}
		if (type == null) {
			return ApiResponse.error(CommonErrorCode.VALIDATION_FAILED, "Type is required");
		}
		return null;
	}

	@Override
	public ApiResponse<LedgerAccount> delete(UUID id) {
		Optional<LedgerAccount> found = this.ledgerAccountRepository.findById(id);
		if (found.isEmpty()) {
			return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No ledger account exists with id " + id);
		}
		LedgerAccount ledgerAccount = found.get();
		this.ledgerAccountRepository.delete(ledgerAccount);
		return ApiResponse.ok(ledgerAccount);
	}

}

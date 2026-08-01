package org.v31bank.ledger.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.v31bank.ledger.domain.constant.LedgerAccountType;
import org.v31bank.ledger.application.dto.LedgerAccountPageQuery;
import org.v31bank.ledger.application.port.in.LedgerAccountUseCase;
import org.v31bank.ledger.application.port.out.LedgerAccountPort;
import org.v31bank.ledger.domain.constant.LedgerAccountStatus;
import org.v31bank.ledger.domain.model.LedgerAccount;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.data.jpa.domain.PageResult;

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
    public ApiResponse<LedgerAccount> update(UUID id, String code, String name, LedgerAccountType type, LedgerAccountStatus status) {
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

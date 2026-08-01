package org.v31bank.notification.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import org.v31bank.core.response.PageResponse;
import org.v31bank.notification.application.dto.LedgerAccountSummary;
import org.v31bank.notification.application.port.in.LedgerAccountUseCase;
import org.v31bank.notification.application.port.out.LedgerAccountPort;

/**
 * Default {@link LedgerAccountUseCase} implementation.
 * <p>
 * No {@code @Transactional}: nothing here touches a database, and the work is a
 * call to another service. A transaction held open around a remote call is a
 * connection held for as long as that service takes to answer.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Service
public class LedgerAccountService implements LedgerAccountUseCase {

    private final LedgerAccountPort ledgerAccountRepository;

    public LedgerAccountService(LedgerAccountPort ledgerAccountRepository) {
        this.ledgerAccountRepository = ledgerAccountRepository;
    }

    @Override
    public LedgerAccountSummary create(String code, String name, String type) {
        return this.ledgerAccountRepository.create(code, name, type);
    }

    @Override
    public Optional<LedgerAccountSummary> get(UUID id) {
        return this.ledgerAccountRepository.findById(id);
    }

    @Override
    public PageResponse<LedgerAccountSummary> page(int pageNumber, int pageSize, String code) {
        return this.ledgerAccountRepository.findPage(pageNumber, pageSize, code);
    }

    @Override
    public LedgerAccountSummary update(UUID id, String code, String name, String type, String status) {
        return this.ledgerAccountRepository.update(id, code, name, type, status);
    }

    @Override
    public LedgerAccountSummary delete(UUID id) {
        return this.ledgerAccountRepository.delete(id);
    }

}

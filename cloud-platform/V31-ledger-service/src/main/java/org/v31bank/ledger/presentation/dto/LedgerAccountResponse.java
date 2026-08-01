package org.v31bank.ledger.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import org.v31bank.ledger.domain.constant.LedgerAccountType;
import org.v31bank.ledger.domain.constant.LedgerAccountStatus;
import org.v31bank.ledger.domain.model.LedgerAccount;

/**
 * API representation of a ledger account.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public record LedgerAccountResponse(UUID id, String code, String name, LedgerAccountType type, LedgerAccountStatus status, Instant createdDate,
        Instant lastModifiedDate) {

    public static LedgerAccountResponse from(LedgerAccount ledgerAccount) {
        return new LedgerAccountResponse(ledgerAccount.getId(), ledgerAccount.getCode(), ledgerAccount.getName(), ledgerAccount.getType(), ledgerAccount.getStatus(),
                ledgerAccount.getCreatedDate(), ledgerAccount.getLastModifiedDate());
    }

}

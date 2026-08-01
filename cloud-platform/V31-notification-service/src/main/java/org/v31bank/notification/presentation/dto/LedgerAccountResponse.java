package org.v31bank.notification.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import org.v31bank.notification.application.dto.LedgerAccountSummary;

/**
 * API representation of a ledger account as this service returns it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public record LedgerAccountResponse(UUID id, String code, String name, String type, String status,
        Instant createdDate, Instant lastModifiedDate) {

    public static LedgerAccountResponse from(LedgerAccountSummary summary) {
        return new LedgerAccountResponse(summary.id(), summary.code(), summary.name(), summary.type(),
                summary.status(), summary.createdDate(), summary.lastModifiedDate());
    }

}

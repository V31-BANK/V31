package org.v31bank.notification.application.port.in;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.core.response.PageResponse;
import org.v31bank.notification.application.dto.LedgerAccountSummary;

/**
 * Use cases for the chart of accounts this service reads from the ledger.
 * <p>
 * Thin today, and deliberately still here: it is the seam where this service
 * will put what it actually needs — caching a catalogue that changes rarely,
 * falling back to the last known copy when the ledger is unreachable, filtering
 * to the accounts a template may name. Calling the port straight from the
 * controller would leave nowhere for any of that to go.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface LedgerAccountUseCase {

    LedgerAccountSummary create(String code, String name, String type);

    Optional<LedgerAccountSummary> get(UUID id);

    PageResponse<LedgerAccountSummary> page(int pageNumber, int pageSize, String code);

    LedgerAccountSummary update(UUID id, String code, String name, String type, String status);

    LedgerAccountSummary delete(UUID id);

}

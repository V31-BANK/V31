package org.v31bank.ledger.application.port.in;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.ledger.domain.constant.LedgerAccountType;
import org.v31bank.ledger.application.dto.LedgerAccountPageQuery;
import org.v31bank.ledger.domain.constant.LedgerAccountStatus;
import org.v31bank.ledger.domain.model.LedgerAccount;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Use cases for managing ledger accounts.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface LedgerAccountUseCase {

    /**
     * Add a ledger account, provided its code is not already taken.
     * @param code the code, unique
     * @param name the display name
     * @param type the type
     * @return the outcome of the command
     */
    ApiResponse<LedgerAccount> create(String code, String name, LedgerAccountType type);

    Optional<LedgerAccount> get(UUID id);

    /**
     * Find a page of ledger accounts matching the filters carried by the query.
     * @param query the filters and the pagination request
     * @return the page of matching records
     */
    PageResult<LedgerAccount> page(LedgerAccountPageQuery query);

    /**
     * Update the ledger account with the given identifier.
     * @param id the record to update
     * @param code the code, which must not belong to another record
     * @param name the display name
     * @param type the type
     * @param status the new status, or {@code null} to leave it unchanged
     * @return the outcome of the command
     */
    ApiResponse<LedgerAccount> update(UUID id, String code, String name, LedgerAccountType type, LedgerAccountStatus status);

    /**
     * Delete the ledger account with the given identifier.
     * @param id the record to delete
     * @return the outcome of the command
     */
    ApiResponse<LedgerAccount> delete(UUID id);

}

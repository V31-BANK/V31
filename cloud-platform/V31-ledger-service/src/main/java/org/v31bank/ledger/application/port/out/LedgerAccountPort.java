package org.v31bank.ledger.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.ledger.application.dto.LedgerAccountPageQuery;
import org.v31bank.ledger.domain.model.LedgerAccount;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Output port for {@link LedgerAccount} persistence, implemented by the infrastructure
 * layer.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface LedgerAccountPort {

    LedgerAccount save(LedgerAccount ledgerAccount);

    Optional<LedgerAccount> findById(UUID id);

    /**
     * Find a page of records matching the filters carried by the query.
     * @param query the filters and the pagination request
     * @return the page of matching records
     */
    PageResult<LedgerAccount> findPage(LedgerAccountPageQuery query);

    /**
     * Whether any record already uses the given code.
     * @param code the value to check
     * @return {@code true} if it is taken
     */
    boolean existsByCode(String code);

    void delete(LedgerAccount ledgerAccount);

}

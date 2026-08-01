package org.v31bank.ledger.infra.persistence.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.v31bank.ledger.domain.model.LedgerAccount;

/**
 * Spring Data JPA repository for {@link LedgerAccount}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface JpaLedgerAccountRepository extends JpaRepository<LedgerAccount, UUID>, JpaSpecificationExecutor<LedgerAccount> {

    boolean existsByCode(String code);

}

package org.v31bank.transfer.infra.persistence.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.v31bank.transfer.domain.model.TransferLimit;

/**
 * Spring Data JPA repository for {@link TransferLimit}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface JpaTransferLimitRepository extends JpaRepository<TransferLimit, UUID>, JpaSpecificationExecutor<TransferLimit> {

    boolean existsByCode(String code);

}

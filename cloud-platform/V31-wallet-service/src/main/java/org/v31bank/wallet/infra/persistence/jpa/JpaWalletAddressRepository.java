package org.v31bank.wallet.infra.persistence.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.v31bank.wallet.domain.model.WalletAddress;

/**
 * Spring Data JPA repository for {@link WalletAddress}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface JpaWalletAddressRepository extends JpaRepository<WalletAddress, UUID>, JpaSpecificationExecutor<WalletAddress> {

    boolean existsByAddress(String address);

}

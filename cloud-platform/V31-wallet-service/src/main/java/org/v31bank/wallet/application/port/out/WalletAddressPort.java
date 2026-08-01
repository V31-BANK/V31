package org.v31bank.wallet.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.wallet.application.dto.WalletAddressPageQuery;
import org.v31bank.wallet.domain.model.WalletAddress;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Output port for {@link WalletAddress} persistence, implemented by the infrastructure
 * layer.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface WalletAddressPort {

    WalletAddress save(WalletAddress walletAddress);

    Optional<WalletAddress> findById(UUID id);

    /**
     * Find a page of records matching the filters carried by the query.
     * @param query the filters and the pagination request
     * @return the page of matching records
     */
    PageResult<WalletAddress> findPage(WalletAddressPageQuery query);

    /**
     * Whether any record already uses the given address.
     * @param address the value to check
     * @return {@code true} if it is taken
     */
    boolean existsByAddress(String address);

    void delete(WalletAddress walletAddress);

}

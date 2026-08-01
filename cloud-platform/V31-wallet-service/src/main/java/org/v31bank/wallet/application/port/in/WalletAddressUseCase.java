package org.v31bank.wallet.application.port.in;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.wallet.application.dto.WalletAddressPageQuery;
import org.v31bank.wallet.domain.constant.WalletAddressStatus;
import org.v31bank.wallet.domain.model.WalletAddress;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Use cases for managing wallet addresss.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface WalletAddressUseCase {

    /**
     * Add a wallet address, provided its address is not already taken.
     * @param address the address, unique
     * @param label the display name
     * @param asset the asset
     * @return the outcome of the command
     */
    ApiResponse<WalletAddress> create(String address, String label, String asset);

    Optional<WalletAddress> get(UUID id);

    /**
     * Find a page of wallet addresss matching the filters carried by the query.
     * @param query the filters and the pagination request
     * @return the page of matching records
     */
    PageResult<WalletAddress> page(WalletAddressPageQuery query);

    /**
     * Update the wallet address with the given identifier.
     * @param id the record to update
     * @param address the address, which must not belong to another record
     * @param label the display name
     * @param asset the asset
     * @param status the new status, or {@code null} to leave it unchanged
     * @return the outcome of the command
     */
    ApiResponse<WalletAddress> update(UUID id, String address, String label, String asset, WalletAddressStatus status);

    /**
     * Delete the wallet address with the given identifier.
     * @param id the record to delete
     * @return the outcome of the command
     */
    ApiResponse<WalletAddress> delete(UUID id);

}

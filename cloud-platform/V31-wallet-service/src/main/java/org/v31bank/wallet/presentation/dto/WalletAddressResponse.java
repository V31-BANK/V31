package org.v31bank.wallet.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import org.v31bank.wallet.domain.constant.WalletAddressStatus;
import org.v31bank.wallet.domain.model.WalletAddress;

/**
 * API representation of a wallet address.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public record WalletAddressResponse(UUID id, String address, String label, String asset, WalletAddressStatus status, Instant createdDate,
        Instant lastModifiedDate) {

    public static WalletAddressResponse from(WalletAddress walletAddress) {
        return new WalletAddressResponse(walletAddress.getId(), walletAddress.getAddress(), walletAddress.getLabel(), walletAddress.getAsset(), walletAddress.getStatus(),
                walletAddress.getCreatedDate(), walletAddress.getLastModifiedDate());
    }

}

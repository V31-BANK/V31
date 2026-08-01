package org.v31bank.wallet.presentation.dto;

import org.v31bank.wallet.domain.constant.WalletAddressStatus;

/**
 * Request body for creating or updating a wallet address.
 *
 * @param address the address, unique
 * @param label the display name
 * @param asset the asset
 * @param status the status; ignored on create, where a record always starts
 * {@code PENDING}, and left unchanged on update when {@code null}
 * @author Xander Wang
 * @since 0.2.0
 */
public record WalletAddressRequest(String address, String label, String asset, WalletAddressStatus status) {

}
